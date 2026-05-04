import React from 'react';
import { motion } from 'motion/react';
import { FilterSettings } from '../../types/editor';
import { Slider } from '../ui/Slider';
import { 
  Sun, 
  Contrast, 
  Droplets, 
  Wind, 
  Moon,
  Palette,
  Maximize,
  Settings,
  MoveHorizontal,
  Maximize2,
  Sparkles
} from 'lucide-react';
import { cn } from '../../lib/utils';

interface SidebarProps {
  filters: FilterSettings;
  onChange: (filters: FilterSettings) => void;
  visible: boolean;
  t: any;
}

const Sidebar: React.FC<SidebarProps> = ({ filters, onChange, visible, t }) => {
  if (!visible) return null;

  const updateFilter = (key: keyof FilterSettings, value: number) => {
    onChange({ ...filters, [key]: value });
  };

  const presets = [
    { 
      name: t.none, 
      color: 'bg-ios-blue', 
      values: { ...filters, brightness: 100, contrast: 100, saturation: 100, sepia: 0, grayscale: 0, blur: 0 } 
    },
    { 
      name: t.vintage, 
      color: 'bg-orange-500', 
      values: { ...filters, brightness: 105, contrast: 110, saturation: 130, sepia: 20, grayscale: 0 } 
    },
    { 
      name: t.noir, 
      color: 'bg-zinc-800', 
      values: { ...filters, brightness: 80, contrast: 140, saturation: 0, grayscale: 100, sepia: 0 } 
    },
    { 
      name: t.cinema, 
      color: 'bg-indigo-600', 
      values: { ...filters, brightness: 90, contrast: 120, saturation: 110, grayscale: 0, sepia: 5 } 
    }
  ];

  const isPresetActive = (presetValues: Partial<FilterSettings>) => {
    return Object.entries(presetValues).every(([key, value]) => filters[key as keyof FilterSettings] === value);
  };

  return (
    <div className="w-80 shrink-0 glass h-full flex flex-col p-6 overflow-y-auto animate-in slide-in-from-right duration-500 border-l border-white/5 shadow-2xl">
      <div className="flex items-center gap-3 mb-10">
        <div className="w-1.5 h-6 bg-ios-blue rounded-full" />
        <h2 className="text-sm font-bold text-white uppercase tracking-widest opacity-40">{t.adjustments}</h2>
      </div>

      <div className="space-y-8">
        <AdjustItem 
          label={t.brightness} 
          icon={<Sun size={14} />} 
          value={filters.brightness} 
          min={0} max={200} 
          onChange={(v) => updateFilter('brightness', v)} 
        />
        <AdjustItem 
          label={t.contrast} 
          icon={<Contrast size={14} />} 
          value={filters.contrast} 
          min={0} max={200} 
          onChange={(v) => updateFilter('contrast', v)} 
        />
        <AdjustItem 
          label={t.saturation} 
          icon={<Droplets size={14} />} 
          value={filters.saturation} 
          min={0} max={200} 
          onChange={(v) => updateFilter('saturation', v)} 
        />
        <AdjustItem 
          label={t.hueRotate} 
          icon={<Palette size={14} />} 
          value={filters.hueRotate} 
          min={0} max={360} 
          onChange={(v) => updateFilter('hueRotate', v)} 
        />
        <AdjustItem 
          label={t.blur} 
          icon={<Wind size={14} />} 
          value={filters.blur} 
          min={0} max={20} 
          onChange={(v) => updateFilter('blur', v)} 
        />
      </div>

      <div className="mt-10 pt-10 border-t border-white/10 space-y-8">
        <div className="flex items-center gap-3 mb-2">
          <div className="w-1.5 h-6 bg-ios-blue rounded-full" />
          <h2 className="text-sm font-bold text-white uppercase tracking-widest opacity-40">{t.transform}</h2>
        </div>
        
        <AdjustItem 
          label={t.skewX} 
          icon={<MoveHorizontal size={14} />} 
          value={filters.skewX} 
          min={-45} max={45} 
          onChange={(v) => updateFilter('skewX', v)} 
        />
        <AdjustItem 
          label={t.skewY} 
          icon={<MoveHorizontal size={14} className="rotate-90" />} 
          value={filters.skewY} 
          min={-45} max={45} 
          onChange={(v) => updateFilter('skewY', v)} 
        />
        <div className="grid grid-cols-2 gap-4">
          <AdjustItem 
            label={t.scaleX} 
            icon={<Maximize size={14} />} 
            value={filters.scaleX} 
            min={0.1} max={3} 
            step={0.1}
            onChange={(v) => updateFilter('scaleX', v)} 
          />
          <AdjustItem 
            label={t.scaleY} 
            icon={<Maximize size={14} className="rotate-90" />} 
            value={filters.scaleY} 
            min={0.1} max={3} 
            step={0.1}
            onChange={(v) => updateFilter('scaleY', v)} 
          />
        </div>
      </div>

      <div className="mt-10 pt-10 border-t border-white/10 space-y-6">
        <div className="flex items-center justify-between mb-2">
          <div className="flex items-center gap-3">
            <div className="w-1.5 h-6 bg-ios-blue rounded-full" />
            <h2 className="text-sm font-bold text-white uppercase tracking-widest opacity-40">{t.filters} {t.grade}</h2>
          </div>
          <Sparkles size={14} className="text-ios-blue opacity-50" />
        </div>
        <div className="grid grid-cols-2 gap-3">
          {presets.map((preset) => (
            <motion.button
              key={preset.name}
              whileHover={{ scale: 1.05, y: -2 }}
              whileTap={{ scale: 0.95 }}
              onClick={() => onChange(preset.values)}
              className={cn(
                "flex items-center gap-3 p-3 rounded-2xl border transition-all duration-300 group",
                isPresetActive(preset.values) 
                  ? "bg-white/10 border-ios-blue/40 shadow-[0_0_15px_rgba(0,122,255,0.2)]" 
                  : "bg-white/5 border-transparent hover:bg-white/10 hover:border-white/10"
              )}
            >
              <div className={cn(
                "w-8 h-8 rounded-full border-2 border-transparent transition-all group-hover:scale-110",
                preset.color,
                isPresetActive(preset.values) && "border-white shadow-[0_0_15px_rgba(255,255,255,0.3)] shadow-ios-blue/50"
              )} />
              <div className="flex flex-col items-start">
                <span className={cn(
                  "text-[10px] font-bold uppercase tracking-widest transition-colors",
                  isPresetActive(preset.values) ? "text-ios-blue drop-shadow-[0_0_8px_rgba(0,122,255,0.4)]" : "text-white/40 group-hover:text-white/70"
                )}>
                  {preset.name}
                </span>
                <span className="text-[8px] font-medium text-white/20 uppercase tracking-tighter group-hover:text-white/30">{t.grade}</span>
              </div>
            </motion.button>
          ))}
        </div>
      </div>

      <div className="mt-auto pt-10">
        <motion.button 
          whileHover={{ scale: 1.02, backgroundColor: 'rgba(255, 255, 255, 0.08)' }}
          whileTap={{ scale: 0.98 }}
          onClick={() => onChange({ 
            brightness: 100, 
            contrast: 100, 
            saturation: 100, 
            blur: 0, 
            sepia: 0, 
            grayscale: 0,
            skewX: 0,
            skewY: 0,
            scaleX: 1,
            scaleY: 1
          })}
          className="w-full py-4 rounded-2xl glass border border-transparent hover:border-white/10 text-white/40 hover:text-white transition-all text-[10px] font-bold tracking-widest uppercase"
        >
          {t.resetAll}
        </motion.button>
      </div>
    </div>
  );
};

const AdjustItem = ({ label, icon, value, min, max, step = 1, onChange }: any) => (
  <div className="space-y-4">
    <div className="flex items-center justify-between">
      <div className="flex items-center gap-2 text-white/40">
        {icon}
        <span className="text-[10px] font-bold uppercase tracking-widest">{label}</span>
      </div>
      <span className="text-xs font-mono text-ios-blue font-bold">{value > 0 ? `+${value}` : value}</span>
    </div>
    <Slider value={value} min={min} max={max} step={step} onChange={onChange} />
  </div>
);

export default Sidebar;
