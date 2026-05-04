import React, { useState, useRef, useEffect } from 'react';
import { motion, AnimatePresence, Reorder } from 'motion/react';
import { 
  Play, 
  Pause, 
  SkipBack, 
  SkipForward, 
  Music, 
  Scissors, 
  Plus,
  Trash2,
  Film,
  Download
} from 'lucide-react';
import { cn } from '../../lib/utils';

interface VideoEditorProps {
  file: File | null;
  filters?: any;
  onUpdateFilters?: (filters: any) => void;
  t: any;
}

interface Clip {
  id: string;
  start: number;
  end: number;
  fileUrl: string;
  duration: number;
  transition?: string;
}

const VideoEditor: React.FC<VideoEditorProps> = ({ file, filters, onUpdateFilters, t }) => {
  const [playing, setPlaying] = useState(false);
  const [currentTime, setCurrentTime] = useState(0);
  const [totalDuration, setTotalDuration] = useState(0);
  const [volume, setVolume] = useState(1);
  const [musicVolume, setMusicVolume] = useState(0.5);
  const [filterIntensity, setFilterIntensity] = useState(100);
  const [activeFilter, setActiveFilter] = useState<'none' | 'vintage' | 'noir' | 'cinema' | 'vivid' | 'cool' | 'warm'>('none');
  const [activeFX, setActiveFX] = useState<string | null>(null);
  const [audioUrl, setAudioUrl] = useState<string | null>(null);
  const [flipX, setFlipX] = useState(false);
  const [flipY, setFlipY] = useState(false);
  const [aspectRatio, setAspectRatio] = useState<'original' | '1:1' | '9:16' | '16:9' | '4:5' | '3:4' | '2:3' | '21:9'>('original');
  const [activeTab, setActiveTab] = useState<'adjust' | 'filters' | 'effects' | 'music' | 'transitions' | 'manage'>('adjust');
  const [exportQuality, setExportQuality] = useState<'144p' | '480p' | '720p' | '1080p' | '2k' | '4k'>('1080p');
  
  const [clips, setClips] = useState<Clip[]>([]);
  const [selectedClipId, setSelectedClipId] = useState<string | null>(null);
  const [pendingVideoClip, setPendingVideoClip] = useState<{ url: string, duration: number } | null>(null);

  const [isExporting, setIsExporting] = useState(false);
  const [exportProgress, setExportProgress] = useState(0);
  const [showExportModal, setShowExportModal] = useState(false);
  const [selectedFormat, setSelectedFormat] = useState<'webm' | 'mp4'>('mp4');
  const exportIntervalRef = useRef<NodeJS.Timeout | null>(null);

  const [trimStart, setTrimStart] = useState(0);
  const [trimEnd, setTrimEnd] = useState(0);

  useEffect(() => {
    if (totalDuration > 0 && (trimEnd === 0 || trimEnd > totalDuration)) {
      setTrimEnd(totalDuration);
    }
  }, [totalDuration]);

  const videoRef = useRef<HTMLVideoElement>(null);
  const audioRef = useRef<HTMLAudioElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const timelineRef = useRef<HTMLDivElement>(null);
  const [timelineHeight, setTimelineHeight] = useState(320);
  const [isResizing, setIsResizing] = useState(false);

  const startResizing = React.useCallback((e: React.MouseEvent) => {
    setIsResizing(true);
    e.preventDefault();
  }, []);

  const stopResizing = React.useCallback(() => {
    setIsResizing(false);
  }, []);

  const resize = React.useCallback((e: MouseEvent) => {
    if (isResizing) {
      const newHeight = window.innerHeight - e.clientY;
      if (newHeight > 150 && newHeight < window.innerHeight * 0.7) {
        setTimelineHeight(newHeight);
      }
    }
  }, [isResizing]);

  useEffect(() => {
    window.addEventListener('mousemove', resize);
    window.addEventListener('mouseup', stopResizing);
    return () => {
      window.removeEventListener('mousemove', resize);
      window.removeEventListener('mouseup', stopResizing);
    };
  }, [resize, stopResizing]);

  const [history, setHistory] = useState<any[]>([]);
  const [historyIndex, setHistoryIndex] = useState(-1);

  const saveToHistory = (newState: any) => {
    // We capture the current state first, then merge the new changes
    const updatedState = {
      clips: newState.clips || clips,
      activeFilter: newState.activeFilter || activeFilter,
      filterIntensity: newState.filterIntensity ?? filterIntensity,
      filters: newState.filters || filters,
      flipX: newState.flipX ?? flipX,
      flipY: newState.flipY ?? flipY,
      aspectRatio: newState.aspectRatio || aspectRatio,
      audioUrl: newState.audioUrl || audioUrl,
    };
    
    const newHistory = history.slice(0, historyIndex + 1);
    newHistory.push(updatedState);
    if (newHistory.length > 50) newHistory.shift();
    setHistory(newHistory);
    setHistoryIndex(newHistory.length - 1);
  };

  const undo = () => {
    if (historyIndex > 0) {
      const prevState = history[historyIndex - 1];
      setClips(prevState.clips);
      setTotalDuration(prevState.clips.reduce((acc: number, c: any) => acc + (c.end - c.start), 0));
      setActiveFilter(prevState.activeFilter);
      setFilterIntensity(prevState.filterIntensity);
      setFlipX(prevState.flipX);
      setFlipY(prevState.flipY);
      setAspectRatio(prevState.aspectRatio);
      setHistoryIndex(historyIndex - 1);
    }
  };

  const redo = () => {
    if (historyIndex < history.length - 1) {
      const nextState = history[historyIndex + 1];
      setClips(nextState.clips);
      setTotalDuration(nextState.clips.reduce((acc: number, c: any) => acc + (c.end - c.start), 0));
      setActiveFilter(nextState.activeFilter);
      setFilterIntensity(nextState.filterIntensity);
      setFlipX(nextState.flipX);
      setFlipY(nextState.flipY);
      setAspectRatio(nextState.aspectRatio);
      setHistoryIndex(historyIndex + 1);
    }
  };

  useEffect(() => {
    if (file) {
      const url = URL.createObjectURL(file);
      const v = document.createElement('video');
      v.src = url;
      v.onloadedmetadata = () => {
        const d = v.duration;
        const initialClip: Clip = { id: 'clip-1', start: 0, end: d, fileUrl: url, duration: d };
        setClips([initialClip]);
        setTotalDuration(d);
        setSelectedClipId(initialClip.id);
      };
      return () => URL.revokeObjectURL(url);
    }
  }, [file]);

  const togglePlay = () => {
    if (!playing && currentTime >= totalDuration - 0.01) {
      seek(0);
    }
    setPlaying(!playing);
  };

  const getCurrentClip = () => {
    let accumulatedTime = 0;
    for (const clip of clips) {
      const duration = clip.end - clip.start;
      if (currentTime >= accumulatedTime && currentTime < accumulatedTime + duration) {
        return clip;
      }
      accumulatedTime += duration;
    }
    return clips[0];
  };

  const activeClip = getCurrentClip();

  const seek = (time: number) => {
    setCurrentTime(time);
    if (videoRef.current) {
      // Find which clip we are in
      let accumulatedTime = 0;
      for (const clip of clips) {
        if (time >= accumulatedTime && time < accumulatedTime + (clip.end - clip.start)) {
          const clipOffset = time - accumulatedTime;
          videoRef.current.src = clip.fileUrl;
          videoRef.current.currentTime = clip.start + clipOffset;
          break;
        }
        accumulatedTime += (clip.end - clip.start);
      }
    }
  };

  const handleAddVideo = (e: React.ChangeEvent<HTMLInputElement>) => {
    const newFile = e.target.files?.[0];
    if (newFile) {
      const url = URL.createObjectURL(newFile);
      const v = document.createElement('video');
      v.src = url;
      v.onloadedmetadata = () => {
        setPendingVideoClip({ url, duration: v.duration });
        setActiveTab('transitions');
        setSelectedClipId(null);
      };
    }
    e.target.value = '';
  };

  const handleApplyTransition = (transitionName: string) => {
    if (pendingVideoClip) {
      const newClip: Clip = {
        id: `clip-${Math.random().toString(36).substr(2, 9)}`,
        start: 0,
        end: pendingVideoClip.duration,
        fileUrl: pendingVideoClip.url,
        duration: pendingVideoClip.duration,
        transition: transitionName
      };
      const updatedClips = [...clips, newClip];
      setClips(updatedClips);
      setTotalDuration(prev => prev + pendingVideoClip.duration);
      saveToHistory({ clips: updatedClips });
      setSelectedClipId(newClip.id);
      setPendingVideoClip(null);
    } else if (selectedClipId) {
      const updatedClips = clips.map(c => c.id === selectedClipId ? { ...c, transition: transitionName } : c);
      setClips(updatedClips);
      saveToHistory({ clips: updatedClips });
    }
  };

  const handleSplit = () => {
    if (!selectedClipId) return;
    const clipIndex = clips.findIndex(c => c.id === selectedClipId);
    if (clipIndex === -1) return;
    const clip = clips[clipIndex];
    
    // Find current time within the selected clip
    let accumulatedTime = 0;
    for (let i = 0; i < clipIndex; i++) {
      accumulatedTime += (clips[i].end - clips[i].start);
    }
    const relativeTime = currentTime - accumulatedTime;
    
    if (relativeTime > 0.1 && relativeTime < (clip.end - clip.start) - 0.1) {
      const splitPoint = clip.start + relativeTime;
      const secondClip: Clip = {
        ...clip,
        id: `clip-${Math.random().toString(36).substr(2, 9)}`,
        start: splitPoint,
      };
      const firstClip: Clip = {
        ...clip,
        end: splitPoint,
      };
      
      const updatedClips = [...clips];
      updatedClips.splice(clipIndex, 1, firstClip, secondClip);
      setClips(updatedClips);
      saveToHistory({ clips: updatedClips });
    }
  };

  const handleDeleteClip = () => {
    if (clips.length <= 1) return;
    const updatedClips = clips.filter(c => c.id !== selectedClipId);
    setClips(updatedClips);
    setTotalDuration(updatedClips.reduce((acc, c) => acc + (c.end - c.start), 0));
    saveToHistory({ clips: updatedClips });
    setSelectedClipId(updatedClips[0]?.id || null);
  };

  const handleClip = () => {
    if (trimStart === 0 && trimEnd === totalDuration) return;

    let newClips: Clip[] = [];
    let currentGlobalTime = 0;

    clips.forEach(clip => {
      const clipDuration = clip.end - clip.start;
      const clipGlobalStart = currentGlobalTime;
      const clipGlobalEnd = currentGlobalTime + clipDuration;

      const overlapStart = Math.max(clipGlobalStart, trimStart);
      const overlapEnd = Math.min(clipGlobalEnd, trimEnd);

      if (overlapStart < overlapEnd) {
        const localStartOffset = overlapStart - clipGlobalStart;
        const localEndOffset = overlapEnd - clipGlobalStart;

        newClips.push({
          ...clip,
          start: clip.start + localStartOffset,
          end: clip.start + localEndOffset,
          duration: localEndOffset - localStartOffset
        });
      }

      currentGlobalTime += clipDuration;
    });

    if (newClips.length > 0) {
      const newTotal = newClips.reduce((acc, c) => acc + (c.end - c.start), 0);
      setClips(newClips);
      setTotalDuration(newTotal);
      setTrimStart(0);
      setTrimEnd(newTotal);
      seek(0);
      saveToHistory({ clips: newClips });
    }
  };

  // Audio playback synchronization
  useEffect(() => {
    if (audioRef.current) {
      audioRef.current.volume = musicVolume;
      if (playing) {
        audioRef.current.currentTime = currentTime;
        audioRef.current.play().catch(() => {});
      } else {
        audioRef.current.pause();
      }
    }
  }, [playing, audioUrl, musicVolume]);

  const [isTransitioning, setIsTransitioning] = useState(false);
  const [transitionType, setTransitionType] = useState<string | null>(null);

  // Video playback synchronization with transitions
  useEffect(() => {
    if (!videoRef.current) return;

    let accumulatedTime = 0;
    let currentFilteredClip: Clip | null = null;

    for (let i = 0; i < clips.length; i++) {
      const duration = clips[i].end - clips[i].start;
      if (currentTime >= accumulatedTime && currentTime < accumulatedTime + duration) {
        currentFilteredClip = clips[i];
        break;
      }
      accumulatedTime += duration;
    }

    if (currentFilteredClip) {
      if (videoRef.current.src !== currentFilteredClip.fileUrl) {
        // Trigger transition effect if we are switching clips
        if (currentFilteredClip.transition && currentFilteredClip.transition !== 'none' && playing) {
          setIsTransitioning(true);
          setTransitionType(currentFilteredClip.transition);
          setTimeout(() => setIsTransitioning(false), 500);
        }
        
        videoRef.current.src = currentFilteredClip.fileUrl;
        videoRef.current.currentTime = currentFilteredClip.start + (currentTime - accumulatedTime);
      }
      
      // Keep playing state in sync
      if (playing && videoRef.current.paused) {
        videoRef.current.play().catch(() => {});
      } else if (!playing && !videoRef.current.paused) {
        videoRef.current.pause();
      }
    }
  }, [currentTime, playing, clips]);

  // Playback Loop
  useEffect(() => {
    let animationFrame: number;
    let lastTime = performance.now();

    const update = () => {
      const now = performance.now();
      const delta = (now - lastTime) / 1000;
      lastTime = now;

      if (playing) {
        const speedMultiplier = activeFX === 'slowmo' ? 0.5 : 1;
        if (videoRef.current) {
          videoRef.current.playbackRate = speedMultiplier;
        }

        setCurrentTime(prev => {
          const next = prev + (delta * speedMultiplier);
          if (next >= totalDuration) {
            setPlaying(false);
            seek(0); // Reset to start
            return 0;
          }
          return next;
        });
      }
      animationFrame = requestAnimationFrame(update);
    };

    animationFrame = requestAnimationFrame(update);
    return () => cancelAnimationFrame(animationFrame);
  }, [playing, totalDuration, activeFX]);

  // Export handling
  useEffect(() => {
    const handleExportStart = () => {
      setShowExportModal(true);
    };
    window.addEventListener('export-request', handleExportStart);
    return () => window.removeEventListener('export-request', handleExportStart);
  }, []);

  const performRealExport = async () => {
    setShowExportModal(false);
    setIsExporting(true);
    setExportProgress(0);

    // Stop current playback
    setPlaying(false);
    seek(0);

    const canvas = document.createElement('canvas');
    const ctx = canvas.getContext('2d');
    const exportVideo = document.createElement('video');
    exportVideo.crossOrigin = "anonymous";
    exportVideo.muted = false;
    exportVideo.volume = 1.0;
    
    if (!ctx) return;

    // Set resolution based on selection
    const resMap: Record<string, [number, number]> = { 
      '144p': [256, 144], 
      '480p': [854, 480], 
      '720p': [1280, 720], 
      '1080p': [1920, 1080], 
      '2k': [2560, 1440], 
      '4k': [3840, 2160] 
    };
    const [w, h] = resMap[exportQuality];
    canvas.width = w;
    canvas.height = h;

    // Audio Setup
    const audioCtx = new (window.AudioContext || (window as any).webkitAudioContext)();
    const destination = audioCtx.createMediaStreamDestination();
    
    // Explicitly resume
    if (audioCtx.state === 'suspended') {
      await audioCtx.resume();
    }
    
    // Attach video to DOM temporarily (some browsers throttle detached media)
    exportVideo.style.position = 'fixed';
    exportVideo.style.top = '-9999px';
    exportVideo.style.left = '-9999px';
    exportVideo.style.width = '1px';
    exportVideo.style.height = '1px';
    exportVideo.muted = false;
    exportVideo.volume = 1.0;
    document.body.appendChild(exportVideo);

    // Better mimeType detection based on user preference
    const mimeTypes = selectedFormat === 'mp4' 
      ? ['video/mp4;codecs=h264,aac', 'video/mp4;codecs=h264,mp4a.40.2', 'video/mp4', 'video/webm;codecs=vp9,opus', 'video/webm']
      : ['video/webm;codecs=vp9,opus', 'video/webm;codecs=vp8,opus', 'video/webm', 'video/mp4;codecs=h264,aac', 'video/mp4'];
    
    let selectedMimeType = '';
    for (const mime of mimeTypes) {
      if (MediaRecorder.isTypeSupported(mime)) {
        selectedMimeType = mime;
        break;
      }
    }

    if (!selectedMimeType) {
      selectedMimeType = MediaRecorder.isTypeSupported('video/webm') ? 'video/webm' : 'video/mp4';
    }

    const canvasStream = canvas.captureStream(30);
    
    // Crucial: Connect to actual hardware destination via silent gain to ensure 
    // audio is processed in all browsers (some throttle if not connected to speakers)
    const silentGain = audioCtx.createGain();
    silentGain.gain.value = 0;
    silentGain.connect(audioCtx.destination);

    // Crucial: Use a Master Gain to ensure consistent audio levels and persistence
    const masterGain = audioCtx.createGain();
    masterGain.gain.value = 1.0;
    masterGain.connect(destination);
    masterGain.connect(silentGain);

    const videoSource = audioCtx.createMediaElementSource(exportVideo);
    videoSource.connect(masterGain);
    
    let tempAudio: HTMLAudioElement | null = null;
    if (audioUrl) {
      tempAudio = new Audio(audioUrl);
      tempAudio.crossOrigin = "anonymous";
      tempAudio.volume = musicVolume;
      const musicSource = audioCtx.createMediaElementSource(tempAudio);
      musicSource.connect(masterGain);
    }

    // Force an audio track if none exists to "wake up" the destination stream
    const osc = audioCtx.createOscillator();
    const oscGain = audioCtx.createGain();
    oscGain.gain.value = 0.0001; // nearly silent but has signal
    osc.connect(oscGain);
    oscGain.connect(destination);
    osc.start();

    // Prepare combined stream
    const combinedStream = new MediaStream();
    canvasStream.getVideoTracks().forEach(track => combinedStream.addTrack(track));
    
    // Wait for bit of audio activity for destination to have tracks
    const audioTracks = destination.stream.getAudioTracks();
    if (audioTracks.length > 0) {
      audioTracks.forEach(track => combinedStream.addTrack(track));
    } else {
      console.warn("No audio tracks found in destination stream initially");
    }

    const mediaRecorder = new MediaRecorder(combinedStream, {
      mimeType: selectedMimeType,
      videoBitsPerSecond: exportQuality === '4k' ? 25000000 : 8000000,
      audioBitsPerSecond: 128000
    });

    const chunks: Blob[] = [];
    mediaRecorder.ondataavailable = (e) => {
        if (e.data && e.data.size > 0) chunks.push(e.data);
    };

    mediaRecorder.onstop = () => {
      osc.stop();
      audioCtx.close();
      if (exportVideo.parentNode) document.body.removeChild(exportVideo);
      const blob = new Blob(chunks, { type: mediaRecorder.mimeType });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      const ext = mediaRecorder.mimeType.includes('mp4') ? 'mp4' : 'webm';
      a.href = url;
      a.download = `lumiere_${exportQuality}_${Date.now()}.${ext}`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
      setIsExporting(false);
      setExportProgress(0);
      setShowExportModal(false);
      alert(t.exportComplete);
    };

    const startRealtimeExport = async () => {
      let exportTime = 0;
      let startTime = 0;
      let isSwitching = false;
      let firstFrameDrawn = false;
      
      const realtimeLoop = async () => {
        if (isSwitching) return;

        if (!firstFrameDrawn) {
          firstFrameDrawn = true;
          // Sync start: ensure recorder is active before media starts
          mediaRecorder.start(100);
          if (tempAudio) tempAudio.play().catch(console.error);
          await exportVideo.play().catch(console.error);
          startTime = performance.now();
        }
        
        const now = performance.now();
        exportTime = (now - startTime) / 1000;
        
        if (exportTime >= totalDuration) {
          if (mediaRecorder.state !== 'inactive') mediaRecorder.stop();
          if (tempAudio) tempAudio.pause();
          exportVideo.pause();
          return;
        }

        // Clip switching logic
        let accumulatedTime = 0;
        let currentClip = clips[0];
        for (const clip of clips) {
          const clipDuration = clip.end - clip.start;
          if (exportTime >= accumulatedTime && exportTime < accumulatedTime + clipDuration) {
            currentClip = clip;
            break;
          }
          accumulatedTime += clipDuration;
        }

        if (exportVideo.src !== currentClip.fileUrl) {
            isSwitching = true;
            exportVideo.src = currentClip.fileUrl;
            exportVideo.currentTime = currentClip.start + Math.max(0, exportTime - accumulatedTime);
            
            await new Promise((resolve) => {
              const onCanPlay = () => {
                exportVideo.removeEventListener('canplay', onCanPlay);
                resolve(null);
              };
              exportVideo.addEventListener('canplay', onCanPlay);
              exportVideo.load();
            });

            await exportVideo.play().catch(console.error);
            isSwitching = false;
            // Precise clock synchronization after clip swap
            startTime = performance.now() - (exportTime * 1000);
        }

        // Draw current frame to canvas
        const f = filters || {};
        const scale = (f.scale || 100) / 100;
        const tx = f.translateX || 0;
        const ty = f.translateY || 0;
        
        ctx.save();
        ctx.clearRect(0, 0, w, h);
        ctx.filter = getFilterStyle();
        
        const drawW = w * scale;
        const drawH = h * scale;
        const drawX = (w - drawW) / 2 + (tx * (w / 100));
        const drawY = (h - drawH) / 2 + (ty * (h / 100));
        
        ctx.drawImage(exportVideo, drawX, drawY, drawW, drawH);
        ctx.restore();

        setExportProgress(Math.min(100, (exportTime / totalDuration) * 100));
        requestAnimationFrame(realtimeLoop);
      };
      
      exportVideo.src = clips[0].fileUrl;
      exportVideo.currentTime = clips[0].start;
      
      try {
        await new Promise((resolve, reject) => {
          const timeout = setTimeout(() => reject(new Error("Timeout waiting for video canplay")), 10000);
          const onCanPlay = () => {
            clearTimeout(timeout);
            exportVideo.removeEventListener('canplay', onCanPlay);
            resolve(null);
          };
          exportVideo.addEventListener('canplay', onCanPlay);
          exportVideo.onerror = (e) => {
            clearTimeout(timeout);
            reject(e);
          };
          exportVideo.load();
        });
        
        realtimeLoop();
      } catch (err) {
        console.error("Export start failed:", err);
        setIsExporting(false);
        alert("Export failed to start. Please try again.");
      }
    };

    startRealtimeExport();
  };

  const handleExport = () => {
    performRealExport();
  };

  const getVideoStyle = () => {
    const f = filters || {};
    const scale = (f.scale || 100) / 100;
    const tx = f.translateX || 0;
    const ty = f.translateY || 0;
    return {
      filter: getFilterStyle(),
      transform: `scale(${scale}) translate(${tx}%, ${ty}%)`,
      transition: 'transform 0.1s ease-out'
    };
  };

  const getFilterStyle = () => {
    const f = filters || {};
    const intensity = filterIntensity / 100;
    let baseFilter = '';
    switch (activeFilter) {
      case 'vintage': baseFilter = `sepia(${0.5 * intensity}) saturate(${1.5 * intensity}) contrast(0.9)`; break;
      case 'noir': baseFilter = `grayscale(${intensity}) contrast(1.1)`; break;
      case 'cinema': baseFilter = `saturate(${1.2 * intensity}) contrast(1.1) brightness(0.9) hue-rotate(-10deg)`; break;
      case 'vivid': baseFilter = `saturate(${1.8 * intensity}) contrast(1.1)`; break;
      case 'cool': baseFilter = `hue-rotate(180deg) saturate(${1.2 * intensity})`; break;
      case 'warm': baseFilter = `sepia(${0.3 * intensity}) hue-rotate(-10deg) saturate(${1.4 * intensity})`; break;
      default: baseFilter = '';
    }
    return `${baseFilter} brightness(${f.brightness || 100}%) contrast(${f.contrast || 100}%) saturate(${f.saturation || 100}%) blur(${f.blur || 0}px)`.trim();
  };

  const handleScrub = (e: React.MouseEvent | React.TouchEvent) => {
    const rect = timelineRef.current?.getBoundingClientRect();
    if (rect) {
      const clientX = 'touches' in e ? e.touches[0].clientX : e.clientX;
      const x = clientX - rect.left;
      const percentage = Math.max(0, Math.min(1, x / rect.width));
      seek(percentage * totalDuration);
    }
  };

  const [isScrubbing, setIsScrubbing] = useState(false);

  useEffect(() => {
    const handleMove = (e: MouseEvent | TouchEvent) => {
      if (isScrubbing) {
        const rect = timelineRef.current?.getBoundingClientRect();
        if (rect) {
          const clientX = 'touches' in e ? (e as TouchEvent).touches[0].clientX : (e as MouseEvent).clientX;
          const x = clientX - rect.left;
          const percentage = Math.max(0, Math.min(1, x / rect.width));
          seek(percentage * totalDuration);
        }
      }
    };
    const handleUp = () => setIsScrubbing(false);

    if (isScrubbing) {
      window.addEventListener('mousemove', handleMove);
      window.addEventListener('touchmove', handleMove);
      window.addEventListener('mouseup', handleUp);
      window.addEventListener('touchend', handleUp);
    }
    return () => {
      window.removeEventListener('mousemove', handleMove);
      window.removeEventListener('touchmove', handleMove);
      window.removeEventListener('mouseup', handleUp);
      window.removeEventListener('touchend', handleUp);
      if (exportIntervalRef.current) clearInterval(exportIntervalRef.current);
    };
  }, [isScrubbing, totalDuration]);

  return (
    <div className="flex-1 flex flex-col bg-black overflow-hidden relative min-h-0">
      {/* Export Format Selector Modal */}
      {showExportModal && (
        <div className="absolute inset-0 z-[110] bg-black/90 backdrop-blur-3xl flex items-center justify-center p-8 animate-in fade-in duration-300">
          <motion.div 
            initial={{ scale: 0.9, opacity: 0, y: 20 }}
            animate={{ scale: 1, opacity: 1, y: 0 }}
            className="w-full max-w-sm glass bg-black/60 border border-white/10 rounded-[2rem] p-8 space-y-8 shadow-2xl"
          >
            <div className="text-center space-y-2">
              <h3 className="text-2xl font-bold tracking-tight text-white">{t.export}</h3>
              <p className="text-white/40 text-[10px] uppercase tracking-widest font-bold">
                {t.filters}, {t.fx}, {t.transitions} & {t.music} {t.apply}
              </p>
            </div>

            <div className="space-y-6">
              <div className="space-y-4">
                <span className="text-[10px] font-bold uppercase tracking-widest text-white/30">{t.format}</span>
                <div className="grid grid-cols-2 gap-4">
                  {['mp4', 'webm'].map((f) => (
                    <button
                      key={f}
                      onClick={() => setSelectedFormat(f as any)}
                      className={cn(
                        "py-4 rounded-2xl border-2 transition-all text-xs font-bold uppercase tracking-widest",
                        selectedFormat === f ? "bg-ios-blue border-ios-blue text-white shadow-lg shadow-blue-500/20" : "bg-white/5 border-transparent text-white/40 hover:bg-white/10"
                      )}
                    >
                      {f}
                    </button>
                  ))}
                </div>
              </div>

              <div className="space-y-4">
                <span className="text-[10px] font-bold uppercase tracking-widest text-white/30">{t.quality}</span>
                <div className="grid grid-cols-3 gap-2">
                  {['144p', '480p', '720p', '1080p', '2k', '4k'].map((q) => (
                    <button
                      key={q}
                      onClick={() => setExportQuality(q as any)}
                      className={cn(
                        "py-3 rounded-xl border transition-all text-[10px] font-bold uppercase tracking-widest",
                        exportQuality === q ? "bg-white text-black border-white" : "glass border-transparent text-white/40 hover:bg-white/5"
                      )}
                    >
                      {q}
                    </button>
                  ))}
                </div>
              </div>
            </div>

            <div className="flex flex-col gap-3 pt-4">
              <button 
                onClick={handleExport}
                className="w-full py-5 bg-ios-blue rounded-2xl text-[10px] font-bold shadow-xl shadow-blue-500/30 hover:scale-[1.02] active:scale-95 transition-all uppercase tracking-[0.2em] text-white"
              >
                {t.beginCreation}
              </button>
              <button 
                onClick={() => setShowExportModal(false)}
                className="w-full py-5 glass text-white/40 hover:text-white rounded-2xl text-[10px] font-bold transition-all uppercase tracking-[0.2em]"
              >
                {t.cancel}
              </button>
            </div>
          </motion.div>
        </div>
      )}

      {/* Export Progress Overlay */}
      {isExporting && (
        <div className="absolute inset-0 z-[120] bg-black/95 backdrop-blur-xl flex flex-col items-center justify-center p-12 gap-8 text-center animate-in fade-in duration-500">
           <div className="relative w-32 h-32">
              <svg className="w-full h-full transform -rotate-90">
                 <circle cx="64" cy="64" r="60" stroke="currentColor" strokeWidth="4" fill="transparent" className="text-white/5" />
                 <circle cx="64" cy="64" r="60" stroke="currentColor" strokeWidth="4" fill="transparent" strokeDasharray={377} strokeDashoffset={377 - (377 * exportProgress) / 100} className="text-ios-blue transition-all duration-300" />
              </svg>
              <div className="absolute inset-0 flex items-center justify-center flex-col">
                 <span className="text-2xl font-black tracking-tighter">{Math.round(exportProgress)}%</span>
              </div>
           </div>
           <div className="space-y-2">
              <h3 className="text-xl font-bold tracking-tight">{t.exporting}</h3>
              <p className="text-white/40 text-sm font-medium">{t.exportSubtitle}</p>
           </div>
        </div>
      )}

      {/* HUD Header */}
      <div className="absolute top-6 left-6 flex gap-2 z-50">
        <button onClick={undo} disabled={historyIndex <= 0} className="glass p-2.5 rounded-xl text-white/40 hover:text-white transition-all disabled:opacity-20"><SkipBack size={20} /></button>
        <button onClick={redo} disabled={historyIndex >= history.length - 1} className="glass p-2.5 rounded-xl text-white/40 hover:text-white transition-all disabled:opacity-20"><SkipForward size={20} /></button>
      </div>

      {/* Preview Area */}
      <div className="flex-1 relative flex items-center justify-center p-8 min-h-0 bg-zinc-950/20">
        <div className="relative w-full h-full glass rounded-[2rem] overflow-hidden shadow-2xl flex items-center justify-center bg-zinc-950">
          {clips.length > 0 && (
            <video 
              ref={videoRef}
              src={activeClip?.fileUrl}
              className="w-full h-full object-contain"
              style={getVideoStyle()}
              onLoadedMetadata={() => {
                if (videoRef.current && activeClip) {
                  let acc = 0;
                  for (const c of clips) {
                    if (c.id === activeClip.id) break;
                    acc += (c.end - c.start);
                  }
                  videoRef.current.currentTime = activeClip.start + (currentTime - acc);
                  if (playing) videoRef.current.play().catch(() => {});
                }
              }}
            />
          )}

          {/* Transition Overlay */}
          <AnimatePresence>
            {(isTransitioning || activeFX) && (
              <motion.div 
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                className={cn(
                    "absolute inset-0 z-50 flex items-center justify-center pointer-events-none",
                    activeFX === 'glitch' ? "bg-white/20 mix-blend-difference scale-110" : "bg-black"
                )}
              >
                {(transitionType === 'zoom' || activeFX === 'zoom') && (
                  <motion.div 
                    initial={{ scale: 0.5, opacity: 0 }}
                    animate={{ scale: 1.5, opacity: 1 }}
                    className="w-full h-full bg-ios-blue/10 flex items-center justify-center"
                  >
                    <div className="w-64 h-64 border-4 border-ios-blue/30 rounded-full animate-ping" />
                  </motion.div>
                )}
                {(transitionType === 'wipe' || activeFX === 'flash') && (
                  <motion.div 
                    initial={{ x: '-100%' }}
                    animate={{ x: '100%' }}
                    transition={{ duration: 0.5, ease: "easeInOut" }}
                    className="absolute inset-0 bg-gradient-to-r from-transparent via-white to-transparent w-full"
                  />
                )}
                {activeFX === 'shake' && (
                    <motion.div 
                        animate={{ x: [0, -10, 10, -10, 10, 0] }}
                        transition={{ repeat: Infinity, duration: 0.2 }}
                        className="w-full h-full border-4 border-red-500/20"
                    />
                )}
                <span className="text-[10px] font-bold text-white/40 uppercase tracking-[0.5em]">{activeFX || transitionType}</span>
              </motion.div>
            )}
          </AnimatePresence>

          {clips.length === 0 && <Film size={48} className="text-white/10" />}
          {audioUrl && <audio ref={audioRef} src={audioUrl} className="hidden" />}
        </div>
      </div>

      {/* Resizer Handle */}
      <div 
        onMouseDown={startResizing}
        className="h-2 w-full cursor-ns-resize hover:bg-ios-blue/30 transition-colors flex items-center justify-center relative z-[100] group"
      >
        <div className="w-16 h-1 bg-white/10 rounded-full group-hover:bg-white/40 transition-colors" />
        <div className="absolute inset-0 -top-2 -bottom-2" /> {/* Invisible hit area expansion */}
      </div>

      {/* Timeline Controls */}
      <div 
        style={{ height: timelineHeight }}
        className="shrink-0 glass border-t border-white/5 p-6 flex flex-col gap-6 select-none bg-black/40 backdrop-blur-3xl overflow-hidden relative"
      >
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-4">
            <div className="flex bg-white/5 p-1 rounded-2xl">
              {['adjust', 'filters', 'effects', 'music', 'transitions', 'manage'].map(tab => (
                <button 
                  key={tab}
                  onClick={() => setActiveTab(tab as any)}
                  className={cn(
                    "px-6 py-2 rounded-xl text-[10px] font-bold tracking-widest transition-all uppercase",
                    activeTab === tab ? "bg-white text-black shadow-lg" : "text-white/40 hover:text-white"
                  )}
                >
                  {t[tab] || tab}
                </button>
              ))}
            </div>

            <AnimatePresence>
              {activeTab === 'adjust' && (
                <motion.div 
                  initial={{ opacity: 0, x: -20 }}
                  animate={{ opacity: 1, x: 0 }}
                  exit={{ opacity: 0, x: -20 }}
                  className="flex items-center gap-6 px-4 py-1.5 glass rounded-2xl border border-white/10"
                >
                  <div className="flex flex-col items-center -space-y-1">
                    <span className="text-[7px] font-mono text-white/30 uppercase tracking-widest leading-none">{t.start || 'Start'}</span>
                    <span className="text-ios-blue text-[10px] font-mono font-bold">{new Date(trimStart * 1000).toISOString().substr(14, 5)}</span>
                  </div>
                  
                  <button 
                    onClick={handleClip}
                    className="flex items-center gap-2 text-white hover:text-ios-blue transition-colors group"
                  >
                    <Scissors size={14} className="group-hover:rotate-12 transition-transform" />
                    <span className="text-[10px] font-bold uppercase tracking-widest">{t.clip || 'Clip'}</span>
                  </button>

                  <div className="flex flex-col items-center -space-y-1">
                    <span className="text-[7px] font-mono text-white/30 uppercase tracking-widest leading-none">{t.end || 'End'}</span>
                    <span className="text-ios-blue text-[10px] font-mono font-bold">{new Date(trimEnd * 1000).toISOString().substr(14, 5)}</span>
                  </div>
                </motion.div>
              )}
            </AnimatePresence>
          </div>
          <div className="flex items-center gap-4 text-xs font-mono text-white/40">
            <span>{new Date(currentTime * 1000).toISOString().substr(14, 5)}</span>
            <div className="w-[1px] h-3 bg-white/20" />
            <span className="text-ios-blue">{new Date(totalDuration * 1000).toISOString().substr(14, 5)}</span>
          </div>
        </div>

        <div className="flex-1 relative min-h-0 overflow-y-auto no-scrollbar">
          <AnimatePresence mode="wait">
            {activeTab === 'adjust' && (
              <div className="flex items-center justify-center h-full pointer-events-none" />
            )}

            {activeTab === 'effects' && (
              <motion.div 
                key="effects"
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -10 }}
                className="flex flex-col gap-6 p-2"
              >
                <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                  {[
                    { label: t.brightness, icon: '☀️', key: 'brightness', min: 0, max: 200 },
                    { label: t.contrast, icon: '🌓', key: 'contrast', min: 0, max: 200 },
                    { label: t.saturation, icon: '🌈', key: 'saturation', min: 0, max: 200 },
                    { label: t.blur, icon: '🌫️', key: 'blur', min: 0, max: 20 },
                    { label: t.scale || 'Scale', icon: '🔍', key: 'scale', min: 10, max: 500, default: 100 },
                    { label: t.translateX || 'Offset X', icon: '↔️', key: 'translateX', min: -100, max: 100, default: 0 },
                    { label: t.translateY || 'Offset Y', icon: '↕️', key: 'translateY', min: -100, max: 100, default: 0 }
                  ].map(control => (
                    <div key={control.key} className="glass p-4 rounded-2xl space-y-3">
                      <div className="flex justify-between items-center text-[9px] font-bold uppercase tracking-widest text-white/40">
                        <span>{control.label}</span>
                        <span className="text-ios-blue">{(filters?.[control.key] ?? (control.default ?? 100))}%</span>
                      </div>
                      <input 
                        type="range"
                        min={control.min}
                        max={control.max}
                        value={filters?.[control.key] ?? (control.default ?? 100)}
                        onChange={(e) => onUpdateFilters?.({ ...filters, [control.key]: parseInt(e.target.value) })}
                        className="w-full h-1 bg-white/10 rounded-full appearance-none cursor-pointer accent-ios-blue"
                      />
                    </div>
                  ))}
                </div>

                <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-5 gap-3">
                  {['slowmo', 'glitch', 'zoom', 'shake', 'flash'].map(effect => (
                    <button 
                      key={effect} 
                      onClick={() => {
                          setActiveFX(effect);
                          setTimeout(() => setActiveFX(null), 1000);
                      }}
                      className={cn(
                          "glass p-6 rounded-2xl flex flex-col items-center gap-3 transition-all group",
                          activeFX === effect ? "bg-ios-blue shadow-lg scale-105" : "hover:bg-ios-blue/10"
                      )}
                    >
                      <div className="w-10 h-10 rounded-xl bg-white/5 flex items-center justify-center group-hover:bg-ios-blue transition-colors">
                        <Plus size={18} />
                      </div>
                      <span className="text-[9px] font-bold tracking-widest uppercase">{t[effect] || effect}</span>
                    </button>
                  ))}
                </div>
              </motion.div>
            )}

            {activeTab === 'manage' && (
              <motion.div 
                key="manage"
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -10 }}
                className="flex flex-col gap-6 p-2"
              >
                <div className="flex flex-col gap-2">
                  <div className="text-[10px] font-bold tracking-widest text-white/30 uppercase">{t.management || 'Clip Management'}</div>
                  <div className="flex gap-3">
                    <button onClick={handleSplit} className="flex-1 py-4 glass rounded-2xl text-[10px] font-bold tracking-widest uppercase hover:bg-white/10 transition-all flex items-center justify-center gap-2">
                      <Scissors size={14} className="text-ios-blue" /> {t.split}
                    </button>
                    <button onClick={handleDeleteClip} className="flex-1 py-4 glass rounded-2xl text-[10px] font-bold tracking-widest uppercase hover:bg-red-500/20 text-red-500 transition-all flex items-center justify-center gap-2">
                      <Trash2 size={14} /> {t.delete}
                    </button>
                  </div>
                  <label className="w-full py-4 bg-ios-blue text-white rounded-2xl text-[10px] font-bold tracking-widest uppercase cursor-pointer hover:opacity-90 active:scale-95 transition-all shadow-lg shadow-blue-500/20 flex items-center justify-center gap-2">
                    <Plus size={16} /> {t.video}
                    <input type="file" accept="video/*" className="hidden" onChange={handleAddVideo} />
                  </label>
                </div>
                
                <div className="glass p-6 rounded-3xl space-y-4">
                  <div className="text-[10px] font-bold tracking-widest text-white/30 uppercase">{t.selectedClip || 'Selected Clip'}</div>
                  {selectedClipId ? (
                    <div className="flex items-center gap-4 bg-white/5 p-4 rounded-2xl">
                      <div className="w-10 h-10 bg-ios-blue/20 rounded-xl flex items-center justify-center text-ios-blue">
                        <Film size={20} />
                      </div>
                      <div className="flex flex-col">
                        <span className="text-[10px] font-bold text-white uppercase tracking-widest">{selectedClipId}</span>
                        <span className="text-[8px] text-white/40 uppercase">
                          {clips.find(c => c.id === selectedClipId)?.duration.toFixed(1)}s • 
                          {clips.find(c => c.id === selectedClipId)?.transition || 'No Transition'}
                        </span>
                      </div>
                    </div>
                  ) : (
                    <div className="text-[9px] text-white/20 uppercase tracking-widest">{t.selectClipToApply || 'Select a clip to edit'}</div>
                  )}
                </div>
              </motion.div>
            )}

            {activeTab === 'filters' && (
              <motion.div 
                key="filters"
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -10 }}
                className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-5 gap-3 p-2"
              >
                {['none', 'vintage', 'noir', 'cinema', 'vivid', 'cool', 'warm'].map(f => (
                  <button 
                    key={f}
                    onClick={() => setActiveFilter(f as any)}
                    className={cn(
                      "glass p-4 rounded-2xl flex flex-col items-center gap-3 border transition-all",
                      activeFilter === f ? "border-ios-blue bg-ios-blue/10" : "border-transparent hover:bg-white/5"
                    )}
                  >
                    <div className={cn("w-12 h-12 rounded-xl flex items-center justify-center", activeFilter === f ? "bg-ios-blue text-white" : "bg-white/5 text-white/40")}>
                      {f === 'none' ? <Film size={20} /> : <div className="w-6 h-6 rounded-full bg-gradient-to-br from-ios-blue to-purple-500" />}
                    </div>
                    <span className="text-[9px] font-bold tracking-widest uppercase">{t[f] || f}</span>
                  </button>
                ))}
              </motion.div>
            )}

            {activeTab === 'music' && (
              <motion.div 
                key="music"
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -10 }}
                className="space-y-6 p-2"
              >
                <div className="glass p-6 rounded-3xl space-y-4">
                  <div className="flex justify-between items-center bg-white/5 p-4 rounded-2xl">
                    <div className="flex items-center gap-4">
                      <div className="w-10 h-10 bg-ios-blue/20 rounded-xl flex items-center justify-center text-ios-blue">
                        <Music size={20} />
                      </div>
                      <div className="flex flex-col">
                        <span className="text-xs font-bold text-white uppercase tracking-widest">Lo-Fi Beats v2</span>
                        <span className="text-[9px] text-white/40 uppercase">3:42 • Ambient Electronic</span>
                      </div>
                    </div>
                    <div className="flex items-center gap-2">
                      <button className="p-2 text-white/40 hover:text-white transition-all"><Trash2 size={16} /></button>
                      <button className="bg-ios-blue px-4 py-1.5 rounded-full text-[9px] font-bold uppercase tracking-widest">{t.swap || 'Swap'}</button>
                    </div>
                  </div>
                  <div className="space-y-3">
                    <div className="flex justify-between text-[8px] font-bold uppercase tracking-[0.2em] text-white/20">
                      <span>Music Volume</span>
                      <span>{Math.round(musicVolume * 100)}%</span>
                    </div>
                    <input 
                      type="range"
                      min="0"
                      max="1"
                      step="0.01"
                      value={musicVolume}
                      onChange={(e) => setMusicVolume(parseFloat(e.target.value))}
                      className="w-full h-1 bg-white/10 rounded-full appearance-none cursor-pointer accent-ios-blue"
                    />
                  </div>
                </div>
              </motion.div>
            )}

            {activeTab === 'transitions' && (
              <motion.div 
                key="transitions"
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -10 }}
                className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-6 gap-3 p-2"
              >
                {['fade', 'dissolve', 'cross', 'wipe', 'slide', 'zoom'].map(key => (
                  <button 
                    key={key} 
                    onClick={() => handleApplyTransition(key)}
                    className="glass p-4 rounded-2xl flex flex-col items-center gap-2 hover:bg-ios-blue/20 hover:border-ios-blue/30 border border-transparent transition-all group"
                  >
                    <div className="w-8 h-8 rounded-lg bg-white/5 flex items-center justify-center text-ios-blue group-hover:bg-ios-blue group-hover:text-white transition-all">
                      <Plus size={16} />
                    </div>
                    <span className="text-[9px] font-bold tracking-widest uppercase text-white/40 group-hover:text-white">{t[key] || key}</span>
                  </button>
                ))}
              </motion.div>
            )}
          </AnimatePresence>
        </div>

        {/* Global Timeline Track & Controls */}
        <div className="flex flex-col gap-4 mt-auto">
          <AnimatePresence>
            {activeTab === 'adjust' && (
              <motion.div
                initial={{ height: 0, opacity: 0 }}
                animate={{ height: 'auto', opacity: 1 }}
                exit={{ height: 0, opacity: 0 }}
                className="overflow-hidden"
              >
                <div 
                  className="h-20 bg-white/5 rounded-2xl relative overflow-hidden track-bg group border border-white/5 shadow-inner cursor-pointer" 
                  ref={timelineRef}
                  onMouseDown={(e) => {
                    const target = e.target as HTMLElement;
                    if (target.closest('.trim-handle')) return;
                    setIsScrubbing(true);
                    handleScrub(e);
                  }}
                  onTouchStart={(e) => {
                    const target = e.target as HTMLElement;
                    if (target.closest('.trim-handle')) return;
                    setIsScrubbing(true);
                    handleScrub(e);
                  }}
                >
                  <div 
                    className="absolute top-0 bottom-0 bg-black/60 z-10 pointer-events-none"
                    style={{ left: 0, width: `${(trimStart / (totalDuration || 1)) * 100}%` }}
                  />
                  <div 
                    className="absolute top-0 bottom-0 bg-black/60 z-10 pointer-events-none"
                    style={{ left: `${(trimEnd / (totalDuration || 1)) * 100}%`, right: 0 }}
                  />

                  <Reorder.Group axis="x" values={clips} onReorder={setClips} className="absolute inset-0 flex">
                    {clips.map(clip => (
                      <Reorder.Item 
                        key={clip.id} 
                        value={clip}
                        className={cn(
                          "relative h-full border-r border-white/10 transition-all flex flex-col justify-end p-2 cursor-grab active:cursor-grabbing",
                          selectedClipId === clip.id ? "bg-ios-blue/20 ring-1 ring-inset ring-ios-blue/50" : "bg-white/5 hover:bg-white/10"
                        )}
                        style={{ width: `${((clip.end - clip.start) / (totalDuration || 1)) * 100}%` }}
                        onClick={(e) => {
                          e.stopPropagation();
                          setSelectedClipId(clip.id);
                        }}
                      >
                        <span className="text-[7px] font-bold text-white/40 uppercase truncate px-1 mb-1">{clip.transition || (clip.id === clips[0].id ? 'Entrance' : 'Cut')}</span>
                        <div className="flex gap-0.5 h-6 opacity-30">
                          {Array.from({ length: Math.ceil(clip.duration) }).map((_, i) => (
                            <div key={i} className="flex-1 bg-white/20 rounded-sm" />
                          ))}
                        </div>
                      </Reorder.Item>
                    ))}
                  </Reorder.Group>
                  
                  {/* Trim Handles */}
                  <div 
                    className="trim-handle absolute top-0 bottom-0 w-4 bg-white/20 hover:bg-white/40 cursor-ew-resize z-40 flex items-center justify-center transition-colors"
                    style={{ left: `${(trimStart / (totalDuration || 1)) * 100}%`, transform: 'translateX(-50%)' }}
                    onMouseDown={(e) => {
                      e.stopPropagation();
                      const handleMove = (moveEvent: MouseEvent) => {
                        const rect = timelineRef.current!.getBoundingClientRect();
                        const x = moveEvent.clientX - rect.left;
                        setTrimStart(Math.max(0, Math.min(trimEnd - 0.1, (x / rect.width) * totalDuration)));
                      };
                      const handleUp = () => {
                        window.removeEventListener('mousemove', handleMove);
                        window.removeEventListener('mouseup', handleUp);
                      };
                      window.addEventListener('mousemove', handleMove);
                      window.addEventListener('mouseup', handleUp);
                    }}
                    onTouchStart={(e) => {
                      e.stopPropagation();
                      const handleMove = (moveEvent: TouchEvent) => {
                        const rect = timelineRef.current!.getBoundingClientRect();
                        const x = moveEvent.touches[0].clientX - rect.left;
                        setTrimStart(Math.max(0, Math.min(trimEnd - 0.1, (x / rect.width) * totalDuration)));
                      };
                      const handleUp = () => {
                        window.removeEventListener('touchmove', handleMove);
                        window.removeEventListener('touchend', handleUp);
                      };
                      window.addEventListener('touchmove', handleMove);
                      window.addEventListener('touchend', handleUp);
                    }}
                  >
                    <div className="w-1.5 h-10 bg-white rounded-full shadow-lg" />
                  </div>

                  <div 
                    className="trim-handle absolute top-0 bottom-0 w-4 bg-white/20 hover:bg-white/40 cursor-ew-resize z-40 flex items-center justify-center transition-colors"
                    style={{ left: `${(trimEnd / (totalDuration || 1)) * 100}%`, transform: 'translateX(-50%)' }}
                    onMouseDown={(e) => {
                      e.stopPropagation();
                      const handleMove = (moveEvent: MouseEvent) => {
                        const rect = timelineRef.current!.getBoundingClientRect();
                        const x = moveEvent.clientX - rect.left;
                        setTrimEnd(Math.max(trimStart + 0.1, Math.min(totalDuration, (x / rect.width) * totalDuration)));
                      };
                      const handleUp = () => {
                        window.removeEventListener('mousemove', handleMove);
                        window.removeEventListener('mouseup', handleUp);
                      };
                      window.addEventListener('mousemove', handleMove);
                      window.addEventListener('mouseup', handleUp);
                    }}
                    onTouchStart={(e) => {
                      e.stopPropagation();
                      const handleMove = (moveEvent: TouchEvent) => {
                        const rect = timelineRef.current!.getBoundingClientRect();
                        const x = moveEvent.touches[0].clientX - rect.left;
                        setTrimEnd(Math.max(trimStart + 0.1, Math.min(totalDuration, (x / rect.width) * totalDuration)));
                      };
                      const handleUp = () => {
                        window.removeEventListener('touchmove', handleMove);
                        window.removeEventListener('touchend', handleUp);
                      };
                      window.addEventListener('touchmove', handleMove);
                      window.addEventListener('touchend', handleUp);
                    }}
                  >
                    <div className="w-1.5 h-10 bg-white rounded-full shadow-lg" />
                  </div>

                  {/* Playhead */}
                  <div 
                    className="absolute top-0 bottom-0 w-[2px] bg-ios-blue shadow-[0_0_15px_rgba(10,132,255,0.8)] z-30 pointer-events-none" 
                    style={{ left: `${(currentTime / (totalDuration || 1)) * 100}%` }}
                  />
                  
                  {/* Time Indicators */}
                  <div className="absolute top-0 inset-x-0 h-4 flex justify-between px-2 text-[6px] font-mono text-white/10 uppercase pointer-events-none">
                    {Array.from({ length: 11 }).map((_, i) => (
                      <span key={i}>{(i * (totalDuration / 10)).toFixed(1)}s</span>
                    ))}
                  </div>
                </div>
              </motion.div>
            )}
          </AnimatePresence>

          <div className="flex items-center gap-3">
            <button onClick={togglePlay} className="w-12 h-12 bg-white text-black rounded-2xl flex items-center justify-center hover:scale-105 active:scale-95 transition-all shadow-xl">
              {playing ? <Pause size={24} /> : <Play size={24} fill="currentColor" />}
            </button>
            <div className="flex-1" />
            <div className="text-[10px] font-mono text-white/20 uppercase tracking-widest">
              {playing ? t.playing : t.paused}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default VideoEditor;
