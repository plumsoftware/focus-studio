/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState, useCallback } from 'react';
import { useDropzone } from 'react-dropzone';
import { motion, AnimatePresence } from 'motion/react';
import { 
  Plus, 
  Image as ImageIcon, 
  Film, 
  X, 
  Download, 
  Moon, 
  Sun,
  LayoutGrid,
  ChevronLeft,
  Languages
} from 'lucide-react';
import { cn } from './lib/utils';
import { EditorState, DEFAULT_FILTERS, FilterSettings } from './types/editor';
import PhotoEditor from './components/Editor/PhotoEditor';
import VideoEditor from './components/Editor/VideoEditor';
import { AdBlock } from './components/AdBlock';
import Sidebar from './components/Editor/Sidebar';
import { Language, translations } from './i18n';

export default function App() {
  const [lang, setLang] = useState<Language>('ru');
  const t = translations[lang];

  const [state, setState] = useState<EditorState>({
    mode: 'idle',
    file: null,
    previewUrl: null,
  });
  const [filters, setFilters] = useState<FilterSettings>(DEFAULT_FILTERS);
  const [showSidebar, setShowSidebar] = useState(true);

  const onDrop = useCallback((acceptedFiles: File[]) => {
    const file = acceptedFiles[0];
    if (!file) return;

    const mode = file.type.startsWith('image/') ? 'photo' : 'video';
    
    setState({
      mode,
      file,
      previewUrl: URL.createObjectURL(file),
    });
  }, []);

  React.useEffect(() => {
    const handleFileChange = (e: any) => {
      onDrop([e.detail]);
    };
    window.addEventListener('app-file-change', handleFileChange);
    return () => window.removeEventListener('app-file-change', handleFileChange);
  }, [onDrop]);

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    accept: {
      'image/*': ['.png', '.jpg', '.jpeg', '.webp'],
      'video/*': ['.mp4', '.mov', '.webm'],
    },
    multiple: false,
  } as any);

  const reset = () => {
    if (state.previewUrl) URL.revokeObjectURL(state.previewUrl);
    setState({ mode: 'idle', file: null, previewUrl: null });
    setFilters(DEFAULT_FILTERS);
  };

  return (
    <div className="h-screen w-full flex flex-col bg-black text-white selection:bg-ios-blue/30 overflow-hidden relative">
      {/* Global Background Glows */}
      <div className="fixed inset-0 pointer-events-none overflow-hidden z-0">
        <div className="absolute top-[-10%] left-[-10%] w-[70%] h-[70%] bg-ios-blue/20 rounded-full blur-[140px] animate-pulse" />
        <div className="absolute bottom-[-10%] right-[-10%] w-[60%] h-[60%] bg-ios-blue/15 rounded-full blur-[120px] animate-pulse delay-700" />
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-full h-full opacity-[0.03]" 
             style={{ backgroundImage: 'radial-gradient(circle, #007AFF 1px, transparent 1px)', backgroundSize: '40px 40px' }} />
      </div>

      {/* Navigation Header - Only shown when file is selected */}
      {state.file && (
        <header className="h-16 glass z-50 px-6 flex items-center justify-between shrink-0 border-b border-white/5">
          <div className="flex items-center gap-4">
            <div className="flex items-center gap-2 mr-2">
              <motion.button 
                whileHover={{ scale: 1.1 }}
                whileTap={{ scale: 0.9 }}
                onClick={() => setLang('ru')}
                className={cn(
                  "w-8 h-8 flex items-center justify-center rounded-full transition-all text-lg",
                  lang === 'ru' ? "bg-ios-blue/30 scale-110 shadow-lg shadow-ios-blue/10 border border-ios-blue/30" : "bg-white/5 opacity-50 hover:opacity-100"
                )}
                title="Русский"
              >
                🇷🇺
              </motion.button>
              <motion.button 
                whileHover={{ scale: 1.1 }}
                whileTap={{ scale: 0.9 }}
                onClick={() => setLang('en')}
                className={cn(
                  "w-8 h-8 flex items-center justify-center rounded-full transition-all text-lg",
                  lang === 'en' ? "bg-ios-blue/30 scale-110 shadow-lg shadow-ios-blue/10 border border-ios-blue/30" : "bg-white/5 opacity-50 hover:opacity-100"
                )}
                title="English"
              >
                🇺🇸
              </motion.button>
            </div>
            <motion.button 
              whileHover={{ scale: 1.1, x: -2 }}
              whileTap={{ scale: 0.9 }}
              onClick={reset}
              className="px-4 py-1.5 text-ios-blue text-sm font-medium hover:text-ios-blue/60 transition-colors"
            >
              {t.cancel}
            </motion.button>
            <div className="h-4 w-[1px] bg-white/10 mx-2" />
            <div className="flex items-center gap-2">
              <span className="font-bold text-lg tracking-tight bg-clip-text text-transparent bg-gradient-to-r from-white to-white/70">
                {t.welcome}
              </span>
              <span className="text-[10px] font-medium opacity-30 uppercase tracking-widest hidden sm:inline">
                • {state.file?.name}
              </span>
            </div>
          </div>

          <div className="flex items-center gap-6">
            <div className="flex bg-white/5 p-1 rounded-xl border border-white/5">
              <motion.button 
                whileHover={{ scale: 1.05 }}
                whileTap={{ scale: 0.95 }}
                onClick={() => setState(prev => ({ ...prev, mode: 'photo' }))}
                className={cn(
                  "px-6 py-2 rounded-lg text-xs font-bold tracking-widest transition-all", 
                  state.mode === 'photo' ? "bg-ios-blue text-white shadow-lg shadow-ios-blue/30 border border-ios-blue/50" : "opacity-40 hover:opacity-60"
                )}
              >
                {t.photo}
              </motion.button>
              <motion.button 
                whileHover={{ scale: 1.05 }}
                whileTap={{ scale: 0.95 }}
                onClick={() => setState(prev => ({ ...prev, mode: 'video' }))}
                className={cn(
                  "px-6 py-2 rounded-lg text-xs font-bold tracking-widest transition-all", 
                  state.mode === 'video' ? "bg-ios-blue text-white shadow-lg shadow-ios-blue/30 border border-ios-blue/50" : "opacity-40 hover:opacity-60"
                )}
              >
                {t.video}
              </motion.button>
            </div>

            <div className="flex items-center gap-3">
              {state.mode === 'photo' && (
                <motion.button 
                  whileHover={{ scale: 1.05 }}
                  whileTap={{ scale: 0.95 }}
                  onClick={() => setShowSidebar(!showSidebar)}
                  className={cn(
                    "p-2.5 rounded-xl transition-all",
                    showSidebar ? "bg-ios-blue/10 text-ios-blue ring-1 ring-ios-blue/20" : "text-white/40 hover:text-white"
                  )}
                >
                  <LayoutGrid size={20} />
                </motion.button>
              )}
              <motion.button 
                whileHover={{ scale: 1.02, boxShadow: '0 0 25px rgba(0, 122, 255, 0.6)' }}
                whileTap={{ scale: 0.98 }}
                onClick={() => window.dispatchEvent(new CustomEvent('export-request'))}
                className="bg-ios-blue px-6 py-2 rounded-full text-sm font-semibold hover:bg-ios-blue/90 transition-all shadow-xl shadow-ios-blue/50"
              >
                {t.export}
              </motion.button>
            </div>
          </div>
        </header>
      )}

      {/* Floating Language Switcher for Landing Page */}
      {!state.file && (
        <div className="fixed top-8 left-8 z-50 flex items-center gap-2 animate-in fade-in slide-in-from-top-4 duration-1000">
          <motion.button 
            whileHover={{ scale: 1.1, y: -2 }}
            whileTap={{ scale: 0.9 }}
            onClick={() => setLang('ru')}
            className={cn(
              "w-12 h-12 flex items-center justify-center rounded-2xl transition-all text-2xl glass border",
              lang === 'ru' ? "bg-ios-blue/20 shadow-xl shadow-ios-blue/10 border-ios-blue/40" : "bg-white/5 opacity-60 hover:opacity-100 border-white/10"
            )}
            title="Русский"
          >
            🇷🇺
          </motion.button>
          <motion.button 
            whileHover={{ scale: 1.1, y: -2 }}
            whileTap={{ scale: 0.9 }}
            onClick={() => setLang('en')}
            className={cn(
              "w-12 h-12 flex items-center justify-center rounded-2xl transition-all text-2xl glass border",
              lang === 'en' ? "bg-ios-blue/20 shadow-xl shadow-ios-blue/10 border-ios-blue/40" : "bg-white/5 opacity-60 hover:opacity-100 border-white/10"
            )}
            title="English"
          >
            🇺🇸
          </motion.button>
        </div>
      )}

      {/* Main Content Area */}
      <main className="flex-1 flex overflow-hidden relative z-10">
        <AnimatePresence mode="wait">
          {(!state.file) ? (
            <main className="flex-1 flex flex-col items-center justify-between p-4 relative z-10 overflow-hidden">
              <div /> {/* Top spacer for layout balance */}
              <div className="w-full max-w-5xl mx-auto py-2">
                <motion.div 
                  initial={{ opacity: 0, scale: 0.95 }}
                  animate={{ opacity: 1, scale: 1 }}
                  transition={{ duration: 1.2, ease: [0.16, 1, 0.3, 1] }}
                  className="flex flex-col items-center text-center gap-10"
                >
                  <div className="flex flex-col gap-4">
                    <motion.div 
                      initial={{ opacity: 0, y: 10 }}
                      animate={{ opacity: 1, y: 0 }}
                      transition={{ delay: 0.3 }}
                      whileHover={{ scale: 1.05, y: -2 }}
                      className="inline-flex mx-auto px-4 py-1.5 rounded-full bg-ios-blue/10 border border-ios-blue/20 text-ios-blue text-[10px] font-bold tracking-[0.25em] uppercase shadow-[0_0_15px_rgba(0,122,255,0.1)]"
                    >
                      {t.professionalStudio}
                    </motion.div>
                    <h1 className="text-6xl md:text-8xl font-black tracking-tighter leading-[0.75] bg-clip-text text-transparent bg-gradient-to-b from-white via-white to-ios-blue/70 pb-6 px-4 drop-shadow-[0_0_20px_rgba(0,122,255,0.2)]">
                      {t.welcome}
                    </h1>
                    <p className="text-lg md:text-xl text-white/50 max-w-xl mx-auto font-medium leading-relaxed">
                      {t.studioDescription}
                    </p>
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-2 gap-6 w-full max-w-2xl px-4">
                    {[
                      { type: 'video', icon: Film, label: t.video, desc: t.editMovies, color: 'bg-ios-blue', accent: 'shadow-ios-blue/40', accept: "video/*" },
                      { type: 'photo', icon: ImageIcon, label: t.photo, desc: t.enhancePhotos, color: 'bg-cyan-500', accent: 'shadow-cyan-500/40', accept: "image/*" }
                    ].map((item, index) => (
                      <motion.label 
                        key={item.type}
                        initial={{ opacity: 0, y: 30 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ delay: 0.5 + index * 0.1, duration: 0.8, ease: [0.16, 1, 0.3, 1] }}
                        whileHover={{ y: -4, scale: 1.01, boxShadow: '0 0 30px rgba(0, 122, 255, 0.15)' }}
                        whileTap={{ scale: 0.98 }}
                        className="group relative glass p-8 rounded-[2rem] border border-white/5 hover:border-ios-blue/40 hover:bg-white/[0.03] transition-all cursor-pointer overflow-hidden shadow-[0_4px_20px_rgba(0,0,0,0.3)] hover:shadow-ios-blue/10"
                      >
                        <input 
                          type="file" 
                          accept={item.accept} 
                          className="hidden" 
                          onChange={(e) => {
                            const file = e.target.files?.[0];
                            if (file) onDrop([file]);
                          }} 
                        />
                        <div className={`absolute top-0 right-0 w-32 h-32 ${item.color}/10 blur-3xl rounded-full -mr-16 -mt-16 group-hover:opacity-100 opacity-0 transition-opacity duration-500`} />
                        
                        <div className="relative flex flex-col items-center gap-6">
                          <div className={`w-16 h-16 rounded-2xl ${item.color} flex items-center justify-center shadow-2xl ${item.accent} group-hover:scale-110 transition-transform duration-500`}>
                            <item.icon size={32} className="text-white" />
                          </div>
                          <div className="flex flex-col gap-2 text-center">
                            <span className="text-2xl font-bold tracking-tight text-white/90">{item.label}</span>
                            <span className="text-xs text-white/40 font-medium max-w-[180px] leading-snug">{item.desc}</span>
                          </div>
                        </div>
                      </motion.label>
                    ))}
                  </div>
                  
                  <motion.div 
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    transition={{ delay: 1, duration: 1 }}
                    className="flex items-center gap-10 opacity-30"
                  >
                    {[
                      { label: "4K RAW", sub: "Ultra HD" },
                      { label: "60 FPS", sub: "Fluid Motion" },
                      { label: "LOG-C", sub: "Dynamic Range" }
                    ].map((spec, i) => (
                      <motion.div 
                        key={i} 
                        whileHover={{ scale: 1.1, opacity: 1 }}
                        className="flex flex-col gap-1.5 items-center cursor-default group/spec transition-opacity"
                      >
                        <span className="text-[10px] font-black tracking-widest uppercase text-white drop-shadow-[0_0_8px_rgba(255,255,255,0.3)]">{spec.label}</span>
                        <div className="w-8 h-[2px] bg-white/20 rounded-full group-hover/spec:bg-ios-blue shadow-[0_0_10px_rgba(0,122,255,0.5)] transition-all" />
                        <span className="text-[7px] font-bold tracking-tighter uppercase opacity-50 group-hover/spec:opacity-100 transition-opacity">{spec.sub}</span>
                      </motion.div>
                    ))}
                  </motion.div>
                </motion.div>
              </div>

              <motion.div
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 1.2 }}
                className="w-full mt-auto"
              >
                <AdBlock />
              </motion.div>
            </main>
          ) : (
            <motion.div 
              key="editor"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              className="flex-1 flex h-full overflow-hidden"
            >
              <div className="flex-1 min-w-0 overflow-hidden relative flex flex-col">
                <div className="absolute top-8 left-8 z-10 glass px-4 py-2 rounded-2xl text-[10px] font-bold tracking-widest text-white/60">
                  {t.rawInput}
                </div>
                {state.mode === 'photo' && state.file ? (
                  <PhotoEditor 
                    file={state.file} 
                    filters={filters} 
                    onUpdateFilters={setFilters}
                    t={t}
                  />
                ) : (
                  <VideoEditor 
                    file={state.file} 
                    filters={filters}
                    onUpdateFilters={setFilters}
                    t={t}
                  />
                )}
              </div>

              {state.mode === 'photo' && (
                <Sidebar 
                  visible={showSidebar}
                  filters={filters}
                  onChange={setFilters}
                  t={t}
                />
              )}
            </motion.div>
          )}
        </AnimatePresence>
      </main>

      {/* Atmospheric background */}
      <div className="fixed inset-0 pointer-events-none overflow-hidden">
        <div className="absolute top-[-20%] right-[-10%] w-[80%] h-[80%] bg-ios-blue/10 blur-[180px] rounded-full animate-pulse" />
        <div className="absolute bottom-[-20%] left-[-10%] w-[80%] h-[80%] bg-indigo-500/10 blur-[180px] rounded-full" />
      </div>
    </div>
  );
}
