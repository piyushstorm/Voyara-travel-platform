import { useEffect, useRef, useState } from 'react';

const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID || '';

// Singleton state to guarantee google.accounts.id.initialize() is called ONLY ONCE
let isGsiScriptLoaded = false;
let isGsiInitialized = false;
let currentSuccessHandler = null;
let currentErrorHandler = null;

function loadGsiScript() {
  if (isGsiScriptLoaded || window.google?.accounts?.id) {
    isGsiScriptLoaded = true;
    return Promise.resolve();
  }

  return new Promise((resolve, reject) => {
    const existingScript = document.querySelector('script[src="https://accounts.google.com/gsi/client"]');
    if (existingScript) {
      isGsiScriptLoaded = true;
      resolve();
      return;
    }

    const script = document.createElement('script');
    script.src = 'https://accounts.google.com/gsi/client';
    script.async = true;
    script.defer = true;
    script.onload = () => {
      isGsiScriptLoaded = true;
      resolve();
    };
    script.onerror = (err) => {
      console.error('Failed to load Google GIS script:', err);
      reject(err);
    };
    document.head.appendChild(script);
  });
}

function initGoogleGsi(clientId) {
  if (isGsiInitialized || !window.google?.accounts?.id) return;

  try {
    window.google.accounts.id.initialize({
      client_id: clientId,
      callback: (response) => {
        // Log ONLY safe metadata per project requirements
        console.log('[Google GIS] Credential received: true, length:', response?.credential ? response.credential.length : 0);
        if (response?.credential && currentSuccessHandler) {
          currentSuccessHandler(response);
        } else if (currentErrorHandler) {
          currentErrorHandler(new Error('No credential received from Google'));
        }
      },
      auto_select: false,
      cancel_on_tap_outside: true,
    });
    isGsiInitialized = true;
    console.log('[Google GIS] Initialized once for origin:', window.location.origin);
  } catch (err) {
    console.error('Failed to initialize Google GIS:', err);
  }
}

export default function GoogleSignInButton({
  onSuccess,
  onError,
  text = 'continue_with',
  shape = 'rectangular',
  theme = 'outline',
  size = 'large',
  width,
}) {
  const containerRef = useRef(null);
  const [ready, setReady] = useState(false);
  const [errorMsg, setErrorMsg] = useState(null);

  // Update active handlers
  currentSuccessHandler = onSuccess;
  currentErrorHandler = onError;

  useEffect(() => {
    if (!GOOGLE_CLIENT_ID) {
      setErrorMsg('Google Client ID not configured.');
      return;
    }

    let isMounted = true;

    loadGsiScript()
      .then(() => {
        if (!isMounted) return;
        initGoogleGsi(GOOGLE_CLIENT_ID);
        setReady(true);
      })
      .catch((err) => {
        if (!isMounted) return;
        setErrorMsg('Unable to load Google Sign-In.');
        if (onError) onError(err);
      });

    return () => {
      isMounted = false;
    };
  }, []);

  useEffect(() => {
    if (!ready || !containerRef.current || !window.google?.accounts?.id) return;

    try {
      containerRef.current.innerHTML = '';
      window.google.accounts.id.renderButton(containerRef.current, {
        type: 'standard',
        shape: shape,
        theme: theme,
        text: text,
        size: size,
        logo_alignment: 'left',
        width: width || (containerRef.current.parentElement?.offsetWidth ? Math.min(containerRef.current.parentElement.offsetWidth, 360) : 320),
      });
    } catch (err) {
      console.error('Error rendering Google Sign-In button:', err);
    }
  }, [ready, text, shape, theme, size, width]);

  if (errorMsg) {
    return (
      <div className="rounded-xl border border-amber-200 bg-amber-50 px-4 py-2.5 text-center text-xs font-medium text-amber-800">
        {errorMsg}
      </div>
    );
  }

  return (
    <div className="w-full flex justify-center items-center my-1 min-h-[44px]">
      <div ref={containerRef} className="w-full flex justify-center" />
      {!ready && (
        <div className="h-10 w-full max-w-[320px] rounded-lg border border-slate-200 bg-slate-50 flex items-center justify-center text-xs font-semibold text-slate-400 animate-pulse">
          Loading Google Sign-In...
        </div>
      )}
    </div>
  );
}
