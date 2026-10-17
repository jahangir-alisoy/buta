import { useCallback, useLayoutEffect, useRef, useState, type MouseEvent, type PointerEvent } from 'react';

const DRAG_THRESHOLD = 4;

interface Gesture {
  pointerId: number;
  startX: number;
  dragging: boolean;
}

function segmentsOf(track: HTMLElement | null): HTMLElement[] {
  return track
    ? Array.from(track.children).filter((child): child is HTMLElement => child instanceof HTMLButtonElement || child instanceof HTMLAnchorElement)
    : [];
}

function segmentAt(segments: HTMLElement[], clientX: number): number {
  let nearest = 0;
  let nearestDistance = Number.POSITIVE_INFINITY;
  segments.forEach((segment, index) => {
    const rect = segment.getBoundingClientRect();
    const distance = clientX < rect.left ? rect.left - clientX : clientX > rect.right ? clientX - rect.right : 0;
    if (distance < nearestDistance) {
      nearest = index;
      nearestDistance = distance;
    }
  });
  return nearest;
}

export function useGlassLens<T extends HTMLElement>(selectedIndex: number, onSelect: (index: number) => void) {
  const trackRef = useRef<T>(null);
  const lensRef = useRef<HTMLSpanElement>(null);
  const gesture = useRef<Gesture | null>(null);
  const suppressClick = useRef(false);
  const [hovered, setHovered] = useState<number | null>(null);
  const [dragging, setDragging] = useState(false);
  const [ready, setReady] = useState(false);

  const place = useCallback((left: number, width: number) => {
    lensRef.current?.style.setProperty('--lens-x', `${left}px`);
    lensRef.current?.style.setProperty('--lens-w', `${width}px`);
  }, []);

  const placeOn = useCallback(
    (index: number) => {
      const segment = segmentsOf(trackRef.current)[index];
      if (segment) {
        place(segment.offsetLeft, segment.offsetWidth);
      }
    },
    [place],
  );

  useLayoutEffect(() => {
    placeOn(selectedIndex);
    const track = trackRef.current;
    if (!track || typeof ResizeObserver === 'undefined') {
      return;
    }
    const observer = new ResizeObserver(() => {
      if (!gesture.current?.dragging) {
        placeOn(selectedIndex);
      }
    });
    observer.observe(track);
    segmentsOf(track).forEach((segment) => observer.observe(segment));
    return () => observer.disconnect();
  }, [selectedIndex, placeOn]);

  useLayoutEffect(() => {
    const frame = requestAnimationFrame(() => setReady(true));
    return () => cancelAnimationFrame(frame);
  }, []);

  const follow = (clientX: number) => {
    const track = trackRef.current;
    const segments = segmentsOf(track);
    if (!track || segments.length === 0) {
      return;
    }
    const index = segmentAt(segments, clientX);
    const width = segments[index].offsetWidth;
    const origin = track.getBoundingClientRect().left + track.clientLeft - track.scrollLeft;
    const first = segments[0].offsetLeft;
    const last = segments[segments.length - 1];
    const left = Math.min(Math.max(clientX - origin - width / 2, first), last.offsetLeft + last.offsetWidth - width);
    place(left, width);
    if (index !== hovered) {
      segments[index].scrollIntoView({ block: 'nearest', inline: 'nearest' });
      setHovered(index);
    }
  };

  const end = (event: PointerEvent<T>, commit: boolean) => {
    const current = gesture.current;
    if (!current || current.pointerId !== event.pointerId) {
      return;
    }
    gesture.current = null;
    if (!current.dragging) {
      return;
    }
    const track = trackRef.current;
    if (track?.hasPointerCapture(event.pointerId)) {
      track.releasePointerCapture(event.pointerId);
    }
    setDragging(false);
    setHovered(null);
    if (!commit) {
      placeOn(selectedIndex);
      return;
    }
    const index = segmentAt(segmentsOf(track), event.clientX);
    suppressClick.current = true;
    placeOn(index);
    if (index !== selectedIndex) {
      onSelect(index);
    }
  };

  const handlers = {
    onPointerDown: (event: PointerEvent<T>) => {
      suppressClick.current = false;
      if (event.button !== 0 || !event.isPrimary) {
        return;
      }
      gesture.current = { pointerId: event.pointerId, startX: event.clientX, dragging: false };
    },
    onPointerMove: (event: PointerEvent<T>) => {
      const current = gesture.current;
      if (!current || current.pointerId !== event.pointerId) {
        return;
      }
      if (!current.dragging) {
        if (Math.abs(event.clientX - current.startX) < DRAG_THRESHOLD) {
          return;
        }
        current.dragging = true;
        trackRef.current?.setPointerCapture(event.pointerId);
        setDragging(true);
      }
      follow(event.clientX);
    },
    onPointerUp: (event: PointerEvent<T>) => end(event, true),
    onPointerCancel: (event: PointerEvent<T>) => end(event, false),
    onLostPointerCapture: (event: PointerEvent<T>) => {
      if (event.target === event.currentTarget) {
        end(event, false);
      }
    },
  };

  const guard = (event: MouseEvent) => {
    if (suppressClick.current && event.detail > 0) {
      suppressClick.current = false;
      event.preventDefault();
      return true;
    }
    return false;
  };

  const select = (index: number, event: MouseEvent) => {
    if (!guard(event)) {
      onSelect(index);
    }
  };

  const trackClassName = ['lens-track', selectedIndex >= 0 && 'has-selection', dragging && 'is-dragging', ready && 'is-ready'].filter(Boolean).join(' ');

  return { trackRef, lensRef, hovered, trackClassName, handlers, select, guard };
}
