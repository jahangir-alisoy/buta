import { useEffect, useRef, useState } from 'react';
import {
  AmbientLight,
  Color,
  DirectionalLight,
  Group,
  IcosahedronGeometry,
  Mesh,
  MeshStandardMaterial,
  PerspectiveCamera,
  Scene,
  TorusGeometry,
  WebGLRenderer,
} from 'three';

function cssColor(name: string, fallback: string): Color {
  const value = getComputedStyle(document.documentElement).getPropertyValue(name).trim();
  try {
    return new Color(value || fallback);
  } catch {
    return new Color(fallback);
  }
}

export default function HeroScene() {
  const host = useRef<HTMLDivElement>(null);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    const container = host.current;
    if (!container) {
      return;
    }
    let renderer: WebGLRenderer;
    try {
      renderer = new WebGLRenderer({ antialias: true, alpha: true, powerPreference: 'low-power' });
    } catch {
      return;
    }
    const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 1.5));
    container.appendChild(renderer.domElement);

    const scene = new Scene();
    const camera = new PerspectiveCamera(40, 1, 0.1, 100);
    camera.position.set(0, 0, 9);

    const accent = cssColor('--color-accent', '#b4233c');
    const accentTwo = cssColor('--color-accent-2', '#e8a33d');
    const group = new Group();
    const ringGeometry = new TorusGeometry(2.1, 0.32, 48, 160);
    const gemGeometry = new IcosahedronGeometry(0.9, 1);
    const ringMaterial = new MeshStandardMaterial({ color: accent, metalness: 0.35, roughness: 0.25 });
    const gemMaterial = new MeshStandardMaterial({ color: accentTwo, metalness: 0.2, roughness: 0.35, flatShading: true });
    const softMaterial = new MeshStandardMaterial({ color: accent.clone().lerp(new Color('#ffffff'), 0.55), metalness: 0.1, roughness: 0.5 });

    const ring = new Mesh(ringGeometry, ringMaterial);
    ring.rotation.set(1.1, 0.3, 0);
    const gem = new Mesh(gemGeometry, gemMaterial);
    gem.position.set(1.9, 1.2, 0.6);
    const small = new Mesh(gemGeometry, softMaterial);
    small.scale.setScalar(0.45);
    small.position.set(-2.1, -1.3, 0.9);
    group.add(ring, gem, small);
    scene.add(group);

    scene.add(new AmbientLight(0xffffff, 0.9));
    const key = new DirectionalLight(0xffffff, 1.6);
    key.position.set(4, 5, 6);
    const rim = new DirectionalLight(accentTwo, 0.8);
    rim.position.set(-5, -2, 3);
    scene.add(key, rim);

    const resize = () => {
      const width = container.clientWidth;
      const height = container.clientHeight;
      if (width === 0 || height === 0) {
        return;
      }
      renderer.setSize(width, height, false);
      camera.aspect = width / height;
      camera.position.z = width < 500 ? 11 : 9;
      camera.updateProjectionMatrix();
    };
    resize();
    const resizeObserver = new ResizeObserver(() => {
      resize();
      renderer.render(scene, camera);
    });
    resizeObserver.observe(container);

    let frame = 0;
    let visible = true;
    const start = performance.now();
    const render = () => {
      const elapsed = (performance.now() - start) / 1000;
      group.rotation.y = Math.sin(elapsed * 0.25) * 0.35;
      ring.rotation.z = elapsed * 0.15;
      gem.rotation.x = elapsed * 0.4;
      gem.position.y = 1.2 + Math.sin(elapsed * 0.8) * 0.15;
      small.position.y = -1.3 + Math.cos(elapsed * 0.7) * 0.12;
      renderer.render(scene, camera);
    };
    const loop = () => {
      render();
      frame = requestAnimationFrame(loop);
    };
    const stop = () => cancelAnimationFrame(frame);
    const play = () => {
      stop();
      if (!reducedMotion && visible && document.visibilityState === 'visible') {
        frame = requestAnimationFrame(loop);
      }
    };
    const intersection = new IntersectionObserver(([entry]) => {
      visible = entry.isIntersecting;
      play();
    });
    intersection.observe(container);
    const onVisibility = () => play();
    document.addEventListener('visibilitychange', onVisibility);

    render();
    setReady(true);
    play();

    return () => {
      stop();
      intersection.disconnect();
      resizeObserver.disconnect();
      document.removeEventListener('visibilitychange', onVisibility);
      ringGeometry.dispose();
      gemGeometry.dispose();
      ringMaterial.dispose();
      gemMaterial.dispose();
      softMaterial.dispose();
      renderer.dispose();
      renderer.domElement.remove();
    };
  }, []);

  return <div ref={host} className="hero-canvas" style={{ opacity: ready ? 1 : 0 }} aria-hidden="true" />;
}
