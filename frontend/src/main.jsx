import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { GoogleOAuthProvider } from '@react-oauth/google'
import './index.css'
import App from './App.jsx'

// Dummy Client ID for development. In production this should be from env vars.
// Real Client ID from Google Cloud Console
const GOOGLE_CLIENT_ID = "923770885408-8oq940hu2r40e3c2uqtgocs3n7737cmb.apps.googleusercontent.com";

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <GoogleOAuthProvider clientId={GOOGLE_CLIENT_ID}>
      <App />
    </GoogleOAuthProvider>
  </StrictMode>,
)
