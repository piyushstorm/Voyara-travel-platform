import { useEffect, useRef, useState } from 'react';

/**
 * Interactive 3D Canvas Globe for Voyara
 * Features:
 * - Latitude / longitude wireframe lines
 * - Atmospheric rim glow
 * - Key global travel hubs with pulsing radar beacons
 * - Animated flight route arcs with moving flight photon particles
 * - Touch & mouse drag interaction with rotation inertia
 * - Zero bundle overhead, 60fps canvas, respects prefers-reduced-motion
 */

// Major global hubs with approximate spherical coordinates (lat, lon in degrees)
const HUBS = [
  { code: 'DEL', name: 'Delhi', lat: 28.556, lon: 77.100, color: '#0f6fff' },
  { code: 'BOM', name: 'Mumbai', lat: 19.089, lon: 72.868, color: '#00d2ff' },
  { code: 'BLR', name: 'Bangalore', lat: 13.198, lon: 77.706, color: '#10b981' },
  { code: 'DXB', name: 'Dubai', lat: 25.253, lon: 55.365, color: '#f59e0b' },
  { code: 'SIN', name: 'Singapore', lat: 1.364, lon: 103.991, color: '#06b6d4' },
  { code: 'LHR', name: 'London', lat: 51.470, lon: -0.454, color: '#3b82f6' },
  { code: 'JFK', name: 'New York', lat: 40.641, lon: -73.778, color: '#8b5cf6' },
  { code: 'HND', name: 'Tokyo', lat: 35.549, lon: 139.779, color: '#ec4899' },
  { code: 'SYD', name: 'Sydney', lat: -33.939, lon: 151.175, color: '#ff6b4a' },
];

const FLIGHT_ROUTES = [
  { from: 0, to: 1 }, // DEL - BOM
  { from: 0, to: 2 }, // DEL - BLR
  { from: 0, to: 3 }, // DEL - DXB
  { from: 1, to: 4 }, // BOM - SIN
  { from: 0, to: 5 }, // DEL - LHR
  { from: 5, to: 6 }, // LHR - JFK
  { from: 4, to: 7 }, // SIN - HND
  { from: 4, to: 8 }, // SIN - SYD
];

export default function InteractiveGlobe({ className = '' }) {
  const canvasRef = useRef(null);
  const [activeHub, setActiveHub] = useState(HUBS[0]);
  const isDraggingRef = useRef(false);
  const lastMouseRef = useRef({ x: 0, y: 0 });
  const rotRef = useRef({ x: 0.35, y: -1.35 });
  const velRef = useRef({ x: 0, y: 0.003 });

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    let animId;
    let width = 0;
    let height = 0;
    let radius = 0;

    const resize = () => {
      const rect = canvas.getBoundingClientRect();
      const dpr = Math.min(window.devicePixelRatio || 1, 2);
      width = rect.width;
      height = rect.height;
      canvas.width = width * dpr;
      canvas.height = height * dpr;
      ctx.scale(dpr, dpr);
      radius = Math.min(width, height) * 0.44;
    };

    resize();
    window.addEventListener('resize', resize);

    // Reduced motion check
    const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    // Convert lat/lon to 3D Cartesian coordinates
    const toCartesian = (lat, lon, r) => {
      const phi = ((90 - lat) * Math.PI) / 180;
      const theta = ((lon + 180) * Math.PI) / 180;
      return {
        x: -r * Math.sin(phi) * Math.cos(theta),
        y: r * Math.cos(phi),
        z: r * Math.sin(phi) * Math.sin(theta),
      };
    };

    // Rotate 3D vector around Y and X axes
    const rotate = (p, rx, ry) => {
      // Rotate Y
      const cosY = Math.cos(ry);
      const sinY = Math.sin(ry);
      const x1 = p.x * cosY + p.z * sinY;
      const z1 = -p.x * sinY + p.z * cosY;

      // Rotate X
      const cosX = Math.cos(rx);
      const sinX = Math.sin(rx);
      const y2 = p.y * cosX - z1 * sinX;
      const z2 = p.y * sinX + z1 * cosX;

      return { x: x1, y: y2, z: z2 };
    };

    let particleTime = 0;

    const render = () => {
      if (!isDraggingRef.current && !prefersReducedMotion) {
        rotRef.current.y += velRef.current.y;
        rotRef.current.x += velRef.current.x;
        // Dampen manual drag velocity
        velRef.current.y = velRef.current.y * 0.96 + 0.003 * 0.04;
        velRef.current.x = velRef.current.x * 0.95;
      }

      particleTime += 0.012;

      ctx.clearRect(0, 0, width, height);
      const cx = width / 2;
      const cy = height / 2;

      // 1. Globe Ambient Glow
      const glowGrad = ctx.createRadialGradient(cx, cy, radius * 0.7, cx, cy, radius * 1.25);
      glowGrad.addColorStop(0, 'rgba(15, 111, 255, 0.12)');
      glowGrad.addColorStop(0.6, 'rgba(2, 176, 206, 0.06)');
      glowGrad.addColorStop(1, 'rgba(15, 111, 255, 0)');
      ctx.fillStyle = glowGrad;
      ctx.beginPath();
      ctx.arc(cx, cy, radius * 1.25, 0, Math.PI * 2);
      ctx.fill();

      // 2. Base Sphere Silhouette
      const sphereGrad = ctx.createRadialGradient(
        cx - radius * 0.35,
        cy - radius * 0.35,
        radius * 0.2,
        cx,
        cy,
        radius
      );
      sphereGrad.addColorStop(0, 'rgba(16, 42, 82, 0.9)');
      sphereGrad.addColorStop(0.7, 'rgba(8, 23, 48, 0.95)');
      sphereGrad.addColorStop(1, 'rgba(4, 14, 30, 1)');
      ctx.fillStyle = sphereGrad;
      ctx.beginPath();
      ctx.arc(cx, cy, radius, 0, Math.PI * 2);
      ctx.fill();

      // Sphere Edge Rim
      ctx.strokeStyle = 'rgba(15, 111, 255, 0.35)';
      ctx.lineWidth = 1.5;
      ctx.stroke();

      // 3. Latitude Lines
      ctx.lineWidth = 1;
      for (let lat = -60; lat <= 60; lat += 30) {
        ctx.beginPath();
        let first = true;
        for (let lon = -180; lon <= 180; lon += 8) {
          const pt = toCartesian(lat, lon, radius);
          const rpt = rotate(pt, rotRef.current.x, rotRef.current.y);
          if (rpt.z > -radius * 0.1) {
            const sx = cx + rpt.x;
            const sy = cy - rpt.y;
            if (first) {
              ctx.moveTo(sx, sy);
              first = false;
            } else {
              ctx.lineTo(sx, sy);
            }
          } else {
            first = true;
          }
        }
        ctx.strokeStyle = 'rgba(56, 189, 248, 0.1)';
        ctx.stroke();
      }

      // 4. Longitude Lines
      for (let lon = -180; lon < 180; lon += 45) {
        ctx.beginPath();
        let first = true;
        for (let lat = -80; lat <= 80; lat += 6) {
          const pt = toCartesian(lat, lon, radius);
          const rpt = rotate(pt, rotRef.current.x, rotRef.current.y);
          if (rpt.z > -radius * 0.1) {
            const sx = cx + rpt.x;
            const sy = cy - rpt.y;
            if (first) {
              ctx.moveTo(sx, sy);
              first = false;
            } else {
              ctx.lineTo(sx, sy);
            }
          } else {
            first = true;
          }
        }
        ctx.strokeStyle = 'rgba(56, 189, 248, 0.1)';
        ctx.stroke();
      }

      // 5. Flight Arcs (Interpolated high arcs)
      FLIGHT_ROUTES.forEach((route, idx) => {
        const h1 = HUBS[route.from];
        const h2 = HUBS[route.to];
        const p1 = toCartesian(h1.lat, h1.lon, radius);
        const p2 = toCartesian(h2.lat, h2.lon, radius);

        const rp1 = rotate(p1, rotRef.current.x, rotRef.current.y);
        const rp2 = rotate(p2, rotRef.current.x, rotRef.current.y);

        // Only draw when at least one point is facing the viewer
        if (rp1.z > -radius * 0.3 || rp2.z > -radius * 0.3) {
          ctx.beginPath();
          const steps = 36;
          for (let i = 0; i <= steps; i++) {
            const t = i / steps;
            // Linear spherical blend + elevated arc altitude
            const mx = p1.x * (1 - t) + p2.x * t;
            const my = p1.y * (1 - t) + p2.y * t;
            const mz = p1.z * (1 - t) + p2.z * t;
            const mag = Math.sqrt(mx * mx + my * my + mz * mz);
            const arcHeight = Math.sin(t * Math.PI) * (radius * 0.16);
            const normScale = (radius + arcHeight) / mag;

            const arcPt = rotate(
              { x: mx * normScale, y: my * normScale, z: mz * normScale },
              rotRef.current.x,
              rotRef.current.y
            );

            const sx = cx + arcPt.x;
            const sy = cy - arcPt.y;
            if (i === 0) ctx.moveTo(sx, sy);
            else ctx.lineTo(sx, sy);
          }
          ctx.strokeStyle = 'rgba(15, 111, 255, 0.38)';
          ctx.lineWidth = 1.4;
          ctx.setLineDash([4, 4]);
          ctx.stroke();
          ctx.setLineDash([]);

          // 6. Flying Light Photon on Arc
          const tParticle = (particleTime + idx * 0.28) % 1;
          const mx = p1.x * (1 - tParticle) + p2.x * tParticle;
          const my = p1.y * (1 - tParticle) + p2.y * tParticle;
          const mz = p1.z * (1 - tParticle) + p2.z * tParticle;
          const mag = Math.sqrt(mx * mx + my * my + mz * mz);
          const arcHeight = Math.sin(tParticle * Math.PI) * (radius * 0.16);
          const normScale = (radius + arcHeight) / mag;

          const photon = rotate(
            { x: mx * normScale, y: my * normScale, z: mz * normScale },
            rotRef.current.x,
            rotRef.current.y
          );

          if (photon.z > 0) {
            const sx = cx + photon.x;
            const sy = cy - photon.y;
            ctx.beginPath();
            ctx.arc(sx, sy, 3, 0, Math.PI * 2);
            ctx.fillStyle = '#00f0ff';
            ctx.shadowColor = '#00f0ff';
            ctx.shadowBlur = 8;
            ctx.fill();
            ctx.shadowBlur = 0;
          }
        }
      });

      // 7. Render Hubs & Beacons
      HUBS.forEach((hub) => {
        const pt = toCartesian(hub.lat, hub.lon, radius);
        const rpt = rotate(pt, rotRef.current.x, rotRef.current.y);

        if (rpt.z > 0) {
          const sx = cx + rpt.x;
          const sy = cy - rpt.y;
          const depthAlpha = Math.min(1, Math.max(0.2, (rpt.z / radius) * 1.2));

          // Radar pulse ripple
          const pulse = (Date.now() / 1200) % 1;
          ctx.beginPath();
          ctx.arc(sx, sy, 4 + pulse * 14, 0, Math.PI * 2);
          ctx.strokeStyle = `rgba(15, 111, 255, ${0.45 * (1 - pulse) * depthAlpha})`;
          ctx.lineWidth = 1.2;
          ctx.stroke();

          // Hub Node Core
          ctx.beginPath();
          ctx.arc(sx, sy, 4, 0, Math.PI * 2);
          ctx.fillStyle = hub.color;
          ctx.shadowColor = hub.color;
          ctx.shadowBlur = 6;
          ctx.fill();
          ctx.shadowBlur = 0;

          // City code pill
          ctx.font = 'bold 10px Inter, system-ui, sans-serif';
          ctx.fillStyle = `rgba(255, 255, 255, ${0.9 * depthAlpha})`;
          ctx.fillText(hub.code, sx + 7, sy + 3);
        }
      });

      animId = requestAnimationFrame(render);
    };

    render();

    // Mouse / Touch handlers for 3D rotation
    const onMouseDown = (e) => {
      isDraggingRef.current = true;
      lastMouseRef.current = { x: e.clientX, y: e.clientY };
    };

    const onMouseMove = (e) => {
      if (!isDraggingRef.current) return;
      const dx = e.clientX - lastMouseRef.current.x;
      const dy = e.clientY - lastMouseRef.current.y;
      lastMouseRef.current = { x: e.clientX, y: e.clientY };

      rotRef.current.y += dx * 0.006;
      rotRef.current.x = Math.max(-0.8, Math.min(0.8, rotRef.current.x - dy * 0.006));
      velRef.current = { x: -dy * 0.001, y: dx * 0.003 };
    };

    const onMouseUp = () => {
      isDraggingRef.current = false;
    };

    const onTouchStart = (e) => {
      if (e.touches.length === 1) {
        isDraggingRef.current = true;
        lastMouseRef.current = { x: e.touches[0].clientX, y: e.touches[0].clientY };
      }
    };

    const onTouchMove = (e) => {
      if (!isDraggingRef.current || e.touches.length !== 1) return;
      const dx = e.touches[0].clientX - lastMouseRef.current.x;
      const dy = e.touches[0].clientY - lastMouseRef.current.y;
      lastMouseRef.current = { x: e.touches[0].clientX, y: e.touches[0].clientY };

      rotRef.current.y += dx * 0.006;
      rotRef.current.x = Math.max(-0.8, Math.min(0.8, rotRef.current.x - dy * 0.006));
      velRef.current = { x: -dy * 0.001, y: dx * 0.003 };
    };

    const onTouchEnd = () => {
      isDraggingRef.current = false;
    };

    canvas.addEventListener('mousedown', onMouseDown);
    window.addEventListener('mousemove', onMouseMove);
    window.addEventListener('mouseup', onMouseUp);

    canvas.addEventListener('touchstart', onTouchStart, { passive: true });
    window.addEventListener('touchmove', onTouchMove, { passive: true });
    window.addEventListener('touchend', onTouchEnd);

    return () => {
      window.removeEventListener('resize', resize);
      cancelAnimationFrame(animId);
      canvas.removeEventListener('mousedown', onMouseDown);
      window.removeEventListener('mousemove', onMouseMove);
      window.removeEventListener('mouseup', onMouseUp);
      canvas.removeEventListener('touchstart', onTouchStart);
      window.removeEventListener('touchmove', onTouchMove);
      window.removeEventListener('touchend', onTouchEnd);
    };
  }, []);

  return (
    <div className={`relative flex flex-col items-center select-none ${className}`}>
      {/* 3D Canvas Element */}
      <div className="relative w-full aspect-square max-w-[440px] cursor-grab active:cursor-grabbing">
        <canvas
          ref={canvasRef}
          className="w-full h-full block rounded-full"
          aria-label="Interactive 3D route globe showing live global flight network"
        />

        {/* Floating status badge */}
        <div className="absolute bottom-2 left-1/2 -translate-x-1/2 bg-slate-900/80 backdrop-blur-md border border-slate-700/80 rounded-full px-3.5 py-1 text-[11px] font-semibold text-sky-200 shadow-lg pointer-events-none flex items-center gap-2 whitespace-nowrap">
          <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
          <span>Interactive Global Network · Drag to explore</span>
        </div>
      </div>

      {/* Hub Quick Selectors */}
      <div className="mt-3 flex flex-wrap justify-center gap-1.5 max-w-sm px-2">
        {HUBS.slice(0, 6).map((hub) => (
          <button
            key={hub.code}
            onClick={() => {
              setActiveHub(hub);
              rotRef.current = {
                x: (hub.lat * Math.PI) / 180 * 0.4,
                y: -((hub.lon + 90) * Math.PI) / 180,
              };
            }}
            className={`px-2.5 py-1 rounded-lg text-xs font-bold transition-all ${
              activeHub.code === hub.code
                ? 'bg-primary text-white shadow-sm'
                : 'bg-white/10 text-slate-300 hover:bg-white/20 hover:text-white'
            }`}
          >
            {hub.code}
          </button>
        ))}
      </div>
    </div>
  );
}
