import { useState, useEffect, useMemo } from "react";

const API = "http://localhost:8080";
const USER_ID = 1;

function StarRating({ rating }) {
  return (
    <div style={{ display: "flex", gap: "2px", alignItems: "center" }}>
      {[1, 2, 3, 4, 5].map((s) => (
        <span key={s} style={{ color: s <= Math.round(rating) ? "#f59e0b" : "#374151", fontSize: "12px" }}>★</span>
      ))}
      <span style={{ color: "#9ca3af", fontSize: "11px", marginLeft: "4px" }}>{rating?.toFixed(1)}</span>
    </div>
  );
}

function ProductCard({ product, onSelect, label }) {
  const [hovered, setHovered] = useState(false);
  return (
    <div
      onClick={() => onSelect(product)}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      style={{
        background: hovered ? "#1a1a2e" : "#12122a",
        border: hovered ? "1px solid #6366f1" : "1px solid #1e1e3f",
        borderRadius: "16px", padding: "20px", cursor: "pointer",
        transition: "all 0.25s ease",
        transform: hovered ? "translateY(-4px)" : "none",
        boxShadow: hovered ? "0 8px 32px rgba(99,102,241,0.25)" : "none",
        position: "relative", display: "flex", flexDirection: "column", gap: "12px",
      }}
    >
      {label && (
        <div style={{
          position: "absolute", top: "12px", right: "12px",
          background: "#6366f1", color: "#fff", fontSize: "10px",
          fontWeight: "700", letterSpacing: "0.08em",
          padding: "3px 8px", borderRadius: "20px", textTransform: "uppercase"
        }}>{label}</div>
      )}
      <div style={{
        width: "100%", height: "160px", background: "#fff", borderRadius: "10px",
        display: "flex", alignItems: "center", justifyContent: "center", overflow: "hidden"
      }}>
        <img src={product.image} alt={product.title}
          style={{ maxHeight: "140px", maxWidth: "100%", objectFit: "contain", padding: "8px" }} />
      </div>
      <div style={{ flex: 1 }}>
        <p style={{
          color: "#e2e8f0", fontSize: "13px", fontWeight: "500", lineHeight: "1.4", margin: "0 0 6px",
          display: "-webkit-box", WebkitLineClamp: 2, WebkitBoxOrient: "vertical", overflow: "hidden"
        }}>{product.title}</p>
        <p style={{
          color: "#818cf8", fontSize: "10px", textTransform: "uppercase",
          letterSpacing: "0.08em", margin: "0 0 6px", fontWeight: "600"
        }}>{product.category}</p>
        <StarRating rating={product.rating?.rate} />
      </div>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
        <span style={{ color: "#34d399", fontWeight: "700", fontSize: "18px" }}>${product.price}</span>
        <span style={{ background: "#1e1e3f", color: "#818cf8", fontSize: "11px", padding: "4px 10px", borderRadius: "20px" }}>View →</span>
      </div>
    </div>
  );
}

function ProductModal({ product, onClose, onInteract }) {
  useEffect(() => { onInteract(product, "VIEW"); }, []);
  return (
    <div style={{
      position: "fixed", inset: 0, background: "rgba(0,0,0,0.85)",
      display: "flex", alignItems: "center", justifyContent: "center", zIndex: 1000, padding: "20px"
    }} onClick={onClose}>
      <div onClick={e => e.stopPropagation()} style={{
        background: "#12122a", border: "1px solid #2d2d5e", borderRadius: "20px",
        padding: "32px", maxWidth: "560px", width: "100%", maxHeight: "90vh", overflowY: "auto"
      }}>
        <button onClick={onClose} style={{ float: "right", background: "none", border: "none", color: "#9ca3af", fontSize: "24px", cursor: "pointer" }}>×</button>
        <div style={{ display: "flex", gap: "24px", flexWrap: "wrap" }}>
          <div style={{ width: "180px", height: "180px", background: "#fff", borderRadius: "12px", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
            <img src={product.image} alt={product.title} style={{ maxHeight: "160px", maxWidth: "160px", objectFit: "contain" }} />
          </div>
          <div style={{ flex: 1, minWidth: "200px" }}>
            <p style={{ color: "#818cf8", fontSize: "11px", textTransform: "uppercase", letterSpacing: "0.1em", margin: "0 0 8px" }}>{product.category}</p>
            <h2 style={{ color: "#e2e8f0", fontSize: "18px", margin: "0 0 12px", lineHeight: "1.4" }}>{product.title}</h2>
            <StarRating rating={product.rating?.rate} />
            <p style={{ color: "#9ca3af", fontSize: "12px", margin: "4px 0 16px" }}>{product.rating?.count} reviews</p>
            <p style={{ color: "#34d399", fontSize: "28px", fontWeight: "700", margin: "0 0 20px" }}>${product.price}</p>
            <div style={{ display: "flex", gap: "10px" }}>
              <button onClick={() => onInteract(product, "CLICK")} style={{ background: "#6366f1", color: "#fff", border: "none", padding: "10px 20px", borderRadius: "10px", cursor: "pointer", fontWeight: "600", fontSize: "14px" }}>Add to Cart</button>
              <button onClick={() => onInteract(product, "PURCHASE")} style={{ background: "#059669", color: "#fff", border: "none", padding: "10px 20px", borderRadius: "10px", cursor: "pointer", fontWeight: "600", fontSize: "14px" }}>Buy Now</button>
            </div>
          </div>
        </div>
        <p style={{ color: "#6b7280", fontSize: "13px", lineHeight: "1.7", marginTop: "20px" }}>{product.description}</p>
      </div>
    </div>
  );
}

export default function App() {
  const [query, setQuery] = useState("");
  const [allProducts, setAllProducts] = useState([]);
  const [recommendations, setRecommendations] = useState([]);
  const [selected, setSelected] = useState(null);
  const [loading, setLoading] = useState(true);
  const [activeCategory, setActiveCategory] = useState("all");
  const [toast, setToast] = useState(null);

  useEffect(() => {
    fetch("https://fakestoreapi.com/products")
      .then(res => res.json())
      .then(data => { setAllProducts(data); setLoading(false); })
      .catch(() => setLoading(false));
  }, []);

  const categories = useMemo(() => {
    const cats = [...new Set(allProducts.map(p => p.category))];
    return ["all", ...cats];
  }, [allProducts]);

  const filtered = useMemo(() => {
    return allProducts.filter(p => {
      const matchesQuery = p.title.toLowerCase().includes(query.toLowerCase()) ||
                           p.category.toLowerCase().includes(query.toLowerCase());
      const matchesCategory = activeCategory === "all" || p.category === activeCategory;
      return matchesQuery && matchesCategory;
    });
  }, [allProducts, query, activeCategory]);

  const showToast = (msg) => { setToast(msg); setTimeout(() => setToast(null), 3000); };

  const handleInteract = async (product, eventType) => {
    try {
      await fetch(`${API}/interact`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ userId: USER_ID, productId: product.id, eventType }),
      });
      showToast(`${eventType === "CLICK" ? "🛒 Added to cart" : eventType === "PURCHASE" ? "✅ Purchased!" : "👁 Viewed"}`);
      fetchRecommendations(product.category);
    } catch (e) { console.error("Interact failed", e); }
  };

  const fetchRecommendations = async (category) => {
    try {
      const res = await fetch(`${API}/products/recommendations?category=${encodeURIComponent(category)}`);
      const data = await res.json();
      setRecommendations(data);
    } catch {
      setRecommendations(allProducts.filter(p => p.category === category).slice(0, 5));
    }
  };

  return (
    <div style={{ minHeight: "100vh", background: "#080818", fontFamily: "'DM Sans', 'Segoe UI', sans-serif", color: "#e2e8f0" }}>
      {/* Sticky Header with Search */}
      <div style={{
        borderBottom: "1px solid #1e1e3f", padding: "16px 40px",
        display: "flex", alignItems: "center", gap: "24px",
        position: "sticky", top: 0, background: "#080818", zIndex: 100
      }}>
        <div style={{ flexShrink: 0 }}>
          <h1 style={{ margin: 0, fontSize: "20px", fontWeight: "700" }}>◈ RecoShop</h1>
          <p style={{ margin: 0, fontSize: "10px", color: "#4b5563", letterSpacing: "0.1em" }}>HYBRID RECOMMENDATION</p>
        </div>

        <div style={{ flex: 1, position: "relative" }}>
          <span style={{ position: "absolute", left: "16px", top: "50%", transform: "translateY(-50%)", color: "#4b5563" }}>🔍</span>
          <input
            value={query}
            onChange={e => setQuery(e.target.value)}
            placeholder="Search products by name or category..."
            style={{
              width: "100%", background: "#12122a", border: "1px solid #2d2d5e",
              borderRadius: "12px", padding: "12px 40px 12px 44px",
              color: "#e2e8f0", fontSize: "14px", outline: "none",
            }}
          />
          {query && (
            <button onClick={() => setQuery("")} style={{
              position: "absolute", right: "14px", top: "50%", transform: "translateY(-50%)",
              background: "none", border: "none", color: "#6b7280", cursor: "pointer", fontSize: "18px"
            }}>×</button>
          )}
        </div>

        <div style={{ background: "#1e1e3f", padding: "6px 14px", borderRadius: "20px", fontSize: "12px", color: "#818cf8", flexShrink: 0 }}>
          User #{USER_ID}
        </div>
      </div>

      <div style={{ maxWidth: "1300px", margin: "0 auto", padding: "28px 40px 60px" }}>
        {/* Category Tabs */}
        <div style={{ display: "flex", gap: "8px", flexWrap: "wrap", marginBottom: "20px" }}>
          {categories.map(cat => (
            <button key={cat} onClick={() => setActiveCategory(cat)} style={{
              background: activeCategory === cat ? "#6366f1" : "#12122a",
              color: activeCategory === cat ? "#fff" : "#9ca3af",
              border: activeCategory === cat ? "1px solid #6366f1" : "1px solid #1e1e3f",
              padding: "7px 16px", borderRadius: "20px", cursor: "pointer",
              fontSize: "12px", fontWeight: "500", textTransform: "capitalize", transition: "all 0.2s"
            }}>{cat}</button>
          ))}
        </div>

        {/* Count */}
        <p style={{ color: "#4b5563", fontSize: "12px", marginBottom: "20px" }}>
          {loading ? "Loading..." : `${filtered.length} product${filtered.length !== 1 ? "s" : ""}`}
          {query && <span style={{ color: "#818cf8" }}> matching "{query}"</span>}
        </p>

        {/* Products Grid */}
        {loading ? (
          <div style={{ textAlign: "center", padding: "80px 0", color: "#374151" }}>Loading products...</div>
        ) : filtered.length === 0 ? (
          <div style={{ textAlign: "center", padding: "80px 0" }}>
            <p style={{ color: "#374151" }}>No products match "{query}"</p>
            <button onClick={() => setQuery("")} style={{ marginTop: "12px", background: "#1e1e3f", color: "#818cf8", border: "none", padding: "8px 20px", borderRadius: "10px", cursor: "pointer" }}>Clear search</button>
          </div>
        ) : (
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(210px, 1fr))", gap: "16px", marginBottom: "48px" }}>
            {filtered.map(p => <ProductCard key={p.id} product={p} onSelect={setSelected} />)}
          </div>
        )}

        {/* Recommendations */}
        {recommendations.length > 0 && (
          <div>
            <div style={{ display: "flex", alignItems: "center", gap: "12px", marginBottom: "20px" }}>
              <div style={{ width: "4px", height: "20px", background: "#6366f1", borderRadius: "2px" }} />
              <h3 style={{ color: "#e2e8f0", fontSize: "16px", fontWeight: "600", margin: 0 }}>Recommended for you</h3>
              <span style={{ background: "#1e1e3f", color: "#818cf8", fontSize: "11px", padding: "3px 10px", borderRadius: "20px" }}>Based on your activity</span>
            </div>
            <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(210px, 1fr))", gap: "16px" }}>
              {recommendations.map(p => <ProductCard key={p.id} product={p} onSelect={setSelected} label="For you" />)}
            </div>
          </div>
        )}
      </div>

      {selected && (
        <ProductModal product={selected} onClose={() => setSelected(null)}
          onInteract={(p, type) => { handleInteract(p, type); setSelected(null); }} />
      )}

      {toast && (
        <div style={{
          position: "fixed", bottom: "24px", right: "24px", background: "#1e3a5f",
          color: "#e2e8f0", padding: "12px 20px", borderRadius: "10px",
          fontSize: "13px", zIndex: 2000, border: "1px solid #1e40af"
        }}>{toast}</div>
      )}
    </div>
  );
}