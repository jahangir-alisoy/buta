import { BookOpen, Bus, Drama, Film, Frame, Landmark, Music, Palette, Sparkles, Tag } from 'lucide-react';

const ICONS = {
  film: Film,
  drama: Drama,
  music: Music,
  palette: Palette,
  landmark: Landmark,
  frame: Frame,
  book: BookOpen,
  bus: Bus,
  sparkles: Sparkles,
};

export function CategoryIcon({ iconKey, size = 20 }: { iconKey: string; size?: number }) {
  const Icon = ICONS[iconKey as keyof typeof ICONS] ?? Tag;
  return <Icon size={size} aria-hidden="true" />;
}
