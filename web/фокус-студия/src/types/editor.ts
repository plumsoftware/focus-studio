export type EditorMode = 'photo' | 'video' | 'idle';

export interface EditorState {
  mode: EditorMode;
  file: File | null;
  previewUrl: string | null;
}

export interface FilterSettings {
  brightness: number;
  contrast: number;
  saturation: number;
  blur: number;
  sepia: number;
  grayscale: number;
  hueRotate: number;
  skewX: number;
  skewY: number;
  scaleX: number;
  scaleY: number;
}

export const DEFAULT_FILTERS: FilterSettings = {
  brightness: 100,
  contrast: 100,
  saturation: 100,
  blur: 0,
  sepia: 0,
  grayscale: 0,
  hueRotate: 0,
  skewX: 0,
  skewY: 0,
  scaleX: 1,
  scaleY: 1,
};
