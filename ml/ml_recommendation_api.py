from flask import Flask, jsonify
import pandas as pd
import numpy as np
from sklearn.metrics.pairwise import cosine_similarity
from sklearn.preprocessing import MinMaxScaler
import psycopg2
import requests

app = Flask(__name__)

# ─────────────────────────────────────────
# Supabase config
# ─────────────────────────────────────────
DB_CONFIG = {
    "host":     "aws-0-us-west-2.pooler.supabase.com",
    "port":     5432,
    "database": "postgres",
    "user":     "postgres.lpdrbizeyqwpkwefmajw",
    "password": "Hybrid@2026SR",
    "sslmode":  "require"
}

FAKESTORE_URL = "https://fakestoreapi.com/products"

# Event weights
EVENT_WEIGHTS = {
    "PURCHASE": 3.0,
    "CART":     2.0,
    "CLICK":    1.0,
    "VIEW":     0.5
}

# ─────────────────────────────────────────
# Fetch interactions from Supabase
# ─────────────────────────────────────────
def fetch_interactions():
    conn = psycopg2.connect(**DB_CONFIG)
    cursor = conn.cursor()
    cursor.execute("SELECT user_id, product_id, event_type FROM interaction;")
    rows = cursor.fetchall()
    cursor.close()
    conn.close()

    if not rows:
        return pd.DataFrame(columns=["user_id", "product_id", "event_type", "weight"])

    df = pd.DataFrame(rows, columns=["user_id", "product_id", "event_type"])
    df["weight"] = df["event_type"].str.upper().map(EVENT_WEIGHTS).fillna(0.5)
    return df

# ─────────────────────────────────────────
# Fetch products from Fake Store API
# ─────────────────────────────────────────
def fetch_products():
    response = requests.get(FAKESTORE_URL)
    products = response.json()
    return pd.DataFrame(products)[["id", "title", "category", "price", "image"]]

# ─────────────────────────────────────────
# Build user-product matrix
# Rows = users, Columns = products, Values = weighted score
# ─────────────────────────────────────────
def build_user_product_matrix(df):
    # Sum weights per user-product pair (user may have clicked multiple times)
    matrix = df.groupby(["user_id", "product_id"])["weight"].sum().reset_index()
    pivot  = matrix.pivot(index="user_id", columns="product_id", values="weight").fillna(0)
    return pivot

# ─────────────────────────────────────────
# COLLABORATIVE FILTERING
# Cosine similarity between users
# ─────────────────────────────────────────
def collaborative_filtering(user_id, matrix, top_n=5):
    if user_id not in matrix.index:
        return {}

    # Compute cosine similarity between all users
    similarity_matrix = cosine_similarity(matrix)
    similarity_df     = pd.DataFrame(
        similarity_matrix,
        index=matrix.index,
        columns=matrix.index
    )

    # Get similarity scores for target user
    user_similarities = similarity_df[user_id].drop(user_id)

    # Weighted sum of other users' interactions
    scores = {}
    for other_user, similarity in user_similarities.items():
        if similarity <= 0:
            continue
        for product_id, rating in matrix.loc[other_user].items():
            if rating > 0 and matrix.loc[user_id, product_id] == 0:
                scores[product_id] = scores.get(product_id, 0) + similarity * rating

    return scores

# ─────────────────────────────────────────
# CONTENT-BASED FILTERING
# TF-IDF style category similarity
# ─────────────────────────────────────────
def content_based_filtering(user_id, interactions_df, products_df):
    if user_id not in interactions_df["user_id"].values:
        return {}

    # Get categories user interacted with and their total weights
    user_interactions = interactions_df[interactions_df["user_id"] == user_id]
    user_interactions = user_interactions.merge(
        products_df[["id", "category"]], left_on="product_id", right_on="id", how="left"
    )

    # Category preference scores
    category_scores = user_interactions.groupby("category")["weight"].sum()

    # Products user has already seen
    seen_products = set(user_interactions["product_id"].values)

    # Score each unseen product by its category
    product_scores = {}
    for _, product in products_df.iterrows():
        if product["id"] in seen_products:
            continue
        category = product["category"]
        score = category_scores.get(category, 0)
        if score > 0:
            product_scores[product["id"]] = score

    return product_scores

# ─────────────────────────────────────────
# HYBRID SCORING
# Combine CF + Content-Based with weights
# ─────────────────────────────────────────
def hybrid_recommend(user_id, top_n=5):
    print(f"\nComputing recommendations for user {user_id}...")

    # Fetch data
    interactions_df = fetch_interactions()
    products_df     = fetch_products()

    if interactions_df.empty:
        print("No interactions found — returning popular products")
        return products_df.head(top_n).to_dict("records")

    # Build matrix
    matrix = build_user_product_matrix(interactions_df)

    # Get all product IDs
    all_product_ids = set(products_df["id"].values)

    # Products user already seen
    if user_id in interactions_df["user_id"].values:
        seen = set(interactions_df[interactions_df["user_id"] == user_id]["product_id"].values)
    else:
        seen = set()

    # Cold start — new user
    if not seen:
        print("New user — returning popular products")
        return products_df.head(top_n).to_dict("records")

    # Run both models
    cf_scores      = collaborative_filtering(user_id, matrix)
    content_scores = content_based_filtering(user_id, interactions_df, products_df)

    # Normalize scores to 0-1 range
    def normalize(scores):
        if not scores:
            return scores
        values = np.array(list(scores.values())).reshape(-1, 1)
        if values.max() == 0:
            return scores
        scaler    = MinMaxScaler()
        normalized = scaler.fit_transform(values).flatten()
        return dict(zip(scores.keys(), normalized))

    cf_norm      = normalize(cf_scores)
    content_norm = normalize(content_scores)

    # Combine with weights: 60% CF + 40% Content
    all_product_ids_unseen = all_product_ids - seen
    hybrid_scores = {}

    for pid in all_product_ids_unseen:
        cf      = cf_norm.get(pid, 0)
        content = content_norm.get(pid, 0)
        hybrid_scores[pid] = (0.6 * cf) + (0.4 * content)

    # Sort by score and get top N
    top_product_ids = sorted(
        hybrid_scores, key=hybrid_scores.get, reverse=True
    )[:top_n]

    print(f"Top {top_n} recommended product IDs: {top_product_ids}")

    # Return full product details
    recommended = products_df[products_df["id"].isin(top_product_ids)]
    return recommended.to_dict("records")


# Connection pool — reuse connections
def get_connection():
    return psycopg2.connect(**DB_CONFIG)

def fetch_interactions():
    try:
        conn = get_connection()
        cursor = conn.cursor()
        cursor.execute("SELECT user_id, product_id, event_type FROM interaction;")
        rows = cursor.fetchall()
        cursor.close()
        conn.close()  # Always close after use
        
        if not rows:
            return pd.DataFrame(columns=["user_id", "product_id", "event_type", "weight"])
        
        df = pd.DataFrame(rows, columns=["user_id", "product_id", "event_type"])
        df["weight"] = df["event_type"].str.upper().map(EVENT_WEIGHTS).fillna(0.5)
        return df
    except Exception as e:
        print(f"DB Error: {e}")
        return pd.DataFrame(columns=["user_id", "product_id", "event_type", "weight"])


# ─────────────────────────────────────────
# FLASK ROUTES
# ─────────────────────────────────────────

@app.route("/recommend/<int:user_id>", methods=["GET"])
def recommend(user_id):
    try:
        recommendations = hybrid_recommend(user_id)
        return jsonify({
            "userId": user_id,
            "recommendations": recommendations,
            "count": len(recommendations)
        })
    except Exception as e:
        print(f"Error: {e}")
        return jsonify({"error": str(e)}), 500


@app.route("/health", methods=["GET"])
def health():
    return jsonify({"status": "ok", "model": "hybrid-CF-contentbased"})


@app.route("/popular", methods=["GET"])
def popular():
    try:
        products_df = fetch_products()
        return jsonify(products_df.head(5).to_dict("records"))
    except Exception as e:
        return jsonify({"error": str(e)}), 500


if __name__ == "__main__":
    print("Starting Hybrid Recommendation ML API...")
    print("Endpoints:")
    print("  GET /recommend/<userId>  — get recommendations")
    print("  GET /health              — health check")
    print("  GET /popular             — popular products")
    app.run(host="0.0.0.0", port=5000, debug=True)