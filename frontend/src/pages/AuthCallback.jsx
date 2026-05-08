import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { supabase } from "../lib/supabaseClient";
import { useAuth } from "../context/AuthContext";
import api from "../api/axios";

export default function AuthCallback() {
  const navigate = useNavigate();
  const { loginUser } = useAuth();

  useEffect(() => {
    const handleCallback = async () => {
      const { data: { session }, error } = await supabase.auth.getSession();
      
      if (error || !session) {
        console.error("No session found:", error);
        navigate("/login");
        return;
      }

      const { user } = session;
      
      try {
        const res = await api.post("/auth/oauth/google", {
          email: user.email,
          name: user.user_metadata?.full_name || user.email.split("@")[0],
          googleId: user.id
        });
        
        const token = res.data.token;
        localStorage.setItem("token", token);
        loginUser(token); // Update auth context
        navigate("/");
      } catch (err) {
        console.error("Backend login failed:", err);
        navigate("/login");
      }
    };

    handleCallback();
  }, [navigate, loginUser]);

  return (
    <div className="flex items-center justify-center h-screen bg-[var(--bg-primary)] text-[var(--accent)]">
      <p>Authenticating...</p>
    </div>
  );
}

