import React from 'react';

interface SliderProps {
  value: number;
  min: number;
  max: number;
  step?: number;
  onChange: (value: number) => void;
}

export const Slider: React.FC<SliderProps> = ({ value, min, max, step = 1, onChange }) => {
  return (
    <div className="relative w-full h-6 flex items-center group">
      <div className="absolute w-full h-1 bg-white/10 rounded-full overflow-hidden">
        <div 
          className="absolute h-full bg-blue-500 rounded-full"
          style={{ width: `${((value - min) / (max - min)) * 100}%` }}
        />
      </div>
      <input
        type="range"
        min={min}
        max={max}
        step={step}
        value={value}
        onChange={(e) => onChange(parseFloat(e.target.value))}
        className="absolute w-full h-full opacity-0 cursor-pointer z-10 appearance-none"
      />
      <div 
        className="absolute w-4 h-4 bg-white rounded-full shadow-lg pointer-events-none transition-transform group-active:scale-90"
        style={{ left: `calc(${((value - min) / (max - min)) * 100}% - 8px)` }}
      />
    </div>
  );
};
