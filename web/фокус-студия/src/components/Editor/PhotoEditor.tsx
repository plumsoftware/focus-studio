import React, { useState, useEffect, useRef } from 'react';
import * as fabric from 'fabric';
import { FilterSettings } from '../../types/editor';
import { motion, AnimatePresence, Reorder } from 'motion/react';
import { 
  Type, 
  Square, 
  Circle, 
  MousePointer2, 
  Undo2, 
  Redo2, 
  Trash2, 
  FlipHorizontal, 
  FlipVertical,
  Layers,
  RotateCw,
  X,
  Lock,
  LockOpen,
  ChevronUp,
  ChevronDown,
  Crop,
  BoxSelect,
  ZoomIn,
  ZoomOut,
  Maximize2,
  Settings2,
  GripVertical,
  Plus,
  Minus,
  MoveHorizontal,
  MoveVertical,
  Image as ImageIcon,
  Upload
} from 'lucide-react';
import { cn } from '../../lib/utils';

interface ToolConfig {
  id: string;
  icon: React.ReactNode;
  label: string;
  type: 'action' | 'toggle' | 'spacer';
  action?: () => void;
  active?: boolean;
  disabled?: boolean;
  color?: string;
}

interface PhotoEditorProps {
  file: File | null;
  filters: FilterSettings;
  onUpdateFilters: (filters: FilterSettings) => void;
  t: any;
}

const PhotoEditor: React.FC<PhotoEditorProps> = ({ file, filters, onUpdateFilters, t }) => {
  const isVideoInPhoto = file && file.type.startsWith('video/');
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const fabricRef = useRef<fabric.Canvas | null>(null);
  const [activeTool, setActiveTool] = useState<'select' | 'text' | 'rect' | 'circle' | 'crop' | 'shadow' | 'distort'>('select');
  const [selectedObject, setSelectedObject] = useState<fabric.Object | null>(null);
  const [currentZoom, setCurrentZoom] = useState(1);
  const [isCustomizingToolbar, setIsCustomizingToolbar] = useState(false);
  const [showExportModal, setShowExportModal] = useState(false);
  const [selectedFormat, setSelectedFormat] = useState<'png' | 'jpeg'>('png');
  const [customFonts, setCustomFonts] = useState<string[]>([]);
  const fontInputRef = useRef<HTMLInputElement>(null);

  // Toolbar Configuration
  const [toolbarItems, setToolbarItems] = useState<string[]>(() => {
    const saved = localStorage.getItem('photo-editor-toolbar-order');
    return saved ? JSON.parse(saved) : [
      'undo', 'redo', 'spacer-1', 'select', 'text', 'rect', 'circle', 'distort', 'spacer-2', 
      'crop', 'spacer-3', 'layers', 'spacer-4', 'flipH', 'flipV', 'rotate', 'shadow', 
      'spacer-5', 'delete'
    ];
  });

  const [hiddenTools, setHiddenTools] = useState<string[]>(() => {
    const saved = localStorage.getItem('photo-editor-hidden-tools');
    return saved ? JSON.parse(saved) : [];
  });

  useEffect(() => {
    localStorage.setItem('photo-editor-toolbar-order', JSON.stringify(toolbarItems));
  }, [toolbarItems]);

  useEffect(() => {
    localStorage.setItem('photo-editor-hidden-tools', JSON.stringify(hiddenTools));
  }, [hiddenTools]);
  const [shadowSettings, setShadowSettings] = useState({
    color: '#000000',
    blur: 10,
    offsetX: 5,
    offsetY: 5,
    opacity: 0.5
  });
  const containerRef = useRef<HTMLDivElement>(null);

  const activeToolRef = useRef(activeTool);
  useEffect(() => {
    activeToolRef.current = activeTool;
  }, [activeTool]);

  const [showLayers, setShowLayers] = useState(false);
  const [layers, setLayers] = useState<fabric.Object[]>([]);
  const [cropRect, setCropRect] = useState<fabric.Rect | null>(null);

  const [history, setHistory] = useState<string[]>([]);
  const [historyIndex, setHistoryIndex] = useState(-1);
  const isStateUpdating = useRef(false);

  useEffect(() => {
    if (activeTool === 'crop') {
      if (!fabricRef.current) return;
      const canvas = fabricRef.current;
      const rect = new fabric.Rect({
        left: 50,
        top: 50,
        width: canvas.getWidth() - 100,
        height: canvas.getHeight() - 100,
        fill: 'transparent',
        stroke: '#0a84ff',
        strokeWidth: 2,
        strokeDashArray: [5, 5],
        cornerColor: '#0a84ff',
        cornerStyle: 'circle',
        transparentCorners: false,
      });
      canvas.add(rect);
      canvas.setActiveObject(rect);
      setCropRect(rect);
      canvas.renderAll();
    } else {
      if (cropRect && fabricRef.current) {
        fabricRef.current.remove(cropRect);
        setCropRect(null);
        fabricRef.current.renderAll();
      }
    }
  }, [activeTool]);

  const applyCrop = () => {
    if (!fabricRef.current || !cropRect) return;
    const canvas = fabricRef.current;
    
    // Get crop area
    const { left = 0, top = 0, width = 1, height = 1 } = cropRect.getBoundingRect();
    
    // Shift all objects
    canvas.getObjects().forEach(obj => {
      if (obj !== cropRect) {
        obj.set({
          left: (obj.left || 0) - left,
          top: (obj.top || 0) - top
        });
        obj.setCoords();
      }
    });

    // Resize canvas
    canvas.setDimensions({ width, height });
    canvas.remove(cropRect);
    setCropRect(null);
    setActiveTool('select');
    canvas.renderAll();
    // saveHistory is called via syncLayers which is triggered by remove
  };

  const setCropAspectRatio = (ratio: number | null) => {
    if (!cropRect || !fabricRef.current) return;
    const canvas = fabricRef.current;
    if (ratio === null) {
      cropRect.set({ lockAspectRatio: false });
    } else {
      const currentWidth = cropRect.getScaledWidth();
      const newHeight = currentWidth / ratio;
      cropRect.set({
        height: newHeight / (cropRect.scaleY || 1),
        lockAspectRatio: true
      });
    }
    fabricRef.current.renderAll();
  };

  const handleExportStart = () => {
    setShowExportModal(true);
  };

  const [canvasDimensions, setCanvasDimensions] = useState({ width: 0, height: 0 });

  const handleExport = () => {
    if (!fabricRef.current) return;
    const canvas = fabricRef.current;
    setShowExportModal(false);
    
    // Discard active object and render to ensure all objects are captured correctly
    canvas.discardActiveObject();
    
    // Store current viewport state
    const vpt = [...(canvas.viewportTransform || [1, 0, 0, 1, 0, 0])];
    const zoom = canvas.getZoom();
    
    // Reset viewport for 1:1 capture of the defined canvas area
    canvas.setViewportTransform([1, 0, 0, 1, 0, 0]);
    canvas.requestRenderAll();
    
    // Use a small delay to ensure the render is processed
    setTimeout(() => {
      try {
        const dataURL = canvas.toDataURL({
          format: selectedFormat,
          quality: 1,
          multiplier: 2
        });
        
        // Restore viewport
        canvas.setViewportTransform(vpt as any);
        canvas.requestRenderAll();
        
        const link = document.createElement('a');
        link.download = `lumiere-edit-${Date.now()}.${selectedFormat}`;
        link.href = dataURL;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
      } catch (err) {
        console.error('Export failed:', err);
        // Ensure restore even on error
        canvas.setViewportTransform(vpt as any);
        canvas.requestRenderAll();
      }
    }, 100);
  };

  useEffect(() => {
    if (!canvasRef.current || !containerRef.current || !file || file.type.startsWith('video/')) return;

    // Initialize fabric canvas
    const canvas = new fabric.Canvas(canvasRef.current, {
      width: containerRef.current.clientWidth,
      height: containerRef.current.clientHeight,
      backgroundColor: 'transparent',
      enableRetinaScaling: true
    });
    fabricRef.current = canvas;
    setCanvasDimensions({ 
      width: containerRef.current.clientWidth, 
      height: containerRef.current.clientHeight 
    });

    const saveHistory = () => {
      if (isStateUpdating.current) return;
      const json = JSON.stringify(canvas.toJSON());
      setHistory(prev => {
        const next = prev.slice(0, historyIndex + 1);
        return [...next, json].slice(-50); // Limit to 50 steps
      });
      setHistoryIndex(prev => prev + 1);
    };

    // Sync layers state
    const syncLayers = () => {
      setLayers([...canvas.getObjects()].reverse());
      saveHistory();
      forceUpdate();
    };

    canvas.on('mouse:over', (e) => {
      if (e.target instanceof fabric.IText || e.target instanceof fabric.Text) {
        canvas.hoverCursor = 'text';
      } else {
        canvas.hoverCursor = 'move';
      }
      canvas.requestRenderAll();
    });

    canvas.on('object:added', syncLayers);
    canvas.on('object:removed', syncLayers);
    canvas.on('object:modified', syncLayers);
    canvas.on('selection:created', (e) => {
      setLayers([...canvas.getObjects()].reverse());
      setSelectedObject(e.selected[0]);
      forceUpdate();
    });
    canvas.on('selection:updated', (e) => {
      setLayers([...canvas.getObjects()].reverse());
      setSelectedObject(e.selected[0]);
      forceUpdate();
    });
    canvas.on('selection:cleared', () => {
      setLayers([...canvas.getObjects()].reverse());
      setSelectedObject(null);
      forceUpdate();
    });

    // Zoom Handling
    canvas.on('mouse:wheel', function(opt) {
      const delta = opt.e.deltaY;
      let zoom = canvas.getZoom();
      zoom *= 0.999 ** delta;
      if (zoom > 20) zoom = 20;
      if (zoom < 0.05) zoom = 0.05;
      
      const point = new fabric.Point(opt.e.offsetX, opt.e.offsetY);
      canvas.zoomToPoint(point, zoom);
      setCurrentZoom(zoom);
      
      opt.e.preventDefault();
      opt.e.stopPropagation();
    });

    // Panning
    let isDragging = false;
    let lastPosX = 0;
    let lastPosY = 0;

    canvas.on('mouse:down', function (opt) {
      const evt = opt.e as any;
      const currentTool = activeToolRef.current;
      if (evt.altKey === true || (currentTool === 'select' && !canvas.getActiveObject())) {
        isDragging = true;
        canvas.selection = false;
        lastPosX = evt.clientX || (evt.touches && evt.touches[0]?.clientX);
        lastPosY = evt.clientY || (evt.touches && evt.touches[0]?.clientY);
      }
    });

    canvas.on('mouse:move', function (opt) {
      if (isDragging) {
        const e = opt.e as any;
        const vpt = canvas.viewportTransform!;
        const currentX = e.clientX || (e.touches && e.touches[0]?.clientX);
        const currentY = e.clientY || (e.touches && e.touches[0]?.clientY);
        vpt[4] += currentX - lastPosX;
        vpt[5] += currentY - lastPosY;
        canvas.requestRenderAll();
        lastPosX = currentX;
        lastPosY = currentY;
      }
    });

    canvas.on('mouse:up', function () {
      if (fabricRef.current) {
        fabricRef.current.setViewportTransform(fabricRef.current.viewportTransform!);
      }
      isDragging = false;
      canvas.selection = true;
    });

    // Load initial image
    const reader = new FileReader();
    reader.onload = async (e) => {
      const data = e.target?.result as string;
      const img = await fabric.FabricImage.fromURL(data);
      
      const scale = Math.min(
        canvas.getWidth() / (img.width || 1),
        canvas.getHeight() / (img.height || 1)
      ) * 0.8;
      
      img.set({
        scaleX: scale,
        scaleY: scale,
        left: canvas.getWidth() / 2,
        top: canvas.getHeight() / 2,
        originX: 'center',
        originY: 'center',
        selectable: true,
        hasControls: true,
        data: { id: `img-${Date.now()}` }
      });

      canvas.add(img);
      canvas.setActiveObject(img);
      canvas.renderAll();
      saveHistory(); // Initial state
    };
    reader.readAsDataURL(file);

    const handleResize = () => {
      if (!containerRef.current || !fabricRef.current) return;
      // Only resize if we haven't cropped (we'll assume the initial dimension match is the signal)
      // Actually, better to check a flag or just use baseDimensions.
      // For now, let's just make it smarter: it shouldn't reset if dimensions are "fixed"
    };

    const handleExportFromEvent = () => handleExportStart();

    window.addEventListener('export-request', handleExportFromEvent);
    window.addEventListener('resize', handleResize);
    
    return () => {
      window.removeEventListener('export-request', handleExportFromEvent);
      window.removeEventListener('resize', handleResize);
      canvas.dispose();
    };
  }, [file]); // Remove selectedFormat from here!
 // Already have handleExportStart and handleExport at component scope

  const undo = async () => {
    if (historyIndex <= 0 || !fabricRef.current) return;
    isStateUpdating.current = true;
    const newIndex = historyIndex - 1;
    await fabricRef.current.loadFromJSON(JSON.parse(history[newIndex]));
    setHistoryIndex(newIndex);
    fabricRef.current.renderAll();
    setLayers([...fabricRef.current.getObjects()].reverse());
    isStateUpdating.current = false;
  };

  const redo = async () => {
    if (historyIndex >= history.length - 1 || !fabricRef.current) return;
    isStateUpdating.current = true;
    const newIndex = historyIndex + 1;
    await fabricRef.current.loadFromJSON(JSON.parse(history[newIndex]));
    setHistoryIndex(newIndex);
    fabricRef.current.renderAll();
    setLayers([...fabricRef.current.getObjects()].reverse());
    isStateUpdating.current = false;
  };

  // Handle fabric filters and transformations update
  useEffect(() => {
    if (!fabricRef.current) return;
    const canvas = fabricRef.current;
    const activeObject = canvas.getActiveObject();
    
    if (activeObject) {
      if (activeObject instanceof fabric.FabricImage) {
        activeObject.filters = [];
        
        if (filters.grayscale > 0) activeObject.filters.push(new fabric.filters.Grayscale());
        if (filters.sepia > 0) activeObject.filters.push(new fabric.filters.Sepia());
        
        activeObject.filters.push(new fabric.filters.Brightness({
          brightness: (filters.brightness - 100) / 100
        }));
        
        activeObject.filters.push(new fabric.filters.Contrast({
          contrast: (filters.contrast - 100) / 100
        }));

        activeObject.applyFilters();
      }

      // Apply transformations to ANY active object
      activeObject.set({
        skewX: filters.skewX,
        skewY: filters.skewY,
        scaleX: filters.scaleX,
        scaleY: filters.scaleY,
      });

      canvas.renderAll();
    }
  }, [filters]);

  const addText = () => {
    if (!fabricRef.current) return;
    const text = new fabric.IText('Tap to edit', {
      left: 100,
      top: 100,
      fontFamily: 'Inter',
      fill: '#ffffff',
      fontSize: 40,
      data: { id: `text-${Date.now()}` }
    });
    fabricRef.current.add(text);
    fabricRef.current.setActiveObject(text);
    setActiveTool('select');
  };

  const addRect = () => {
    if (!fabricRef.current) return;
    const rect = new fabric.Rect({
      left: 100,
      top: 100,
      fill: 'rgba(255,255,255,0.5)',
      width: 150,
      height: 100,
      stroke: '#ffffff',
      strokeWidth: 2,
      rx: 12,
      ry: 12,
      data: { id: `rect-${Date.now()}` }
    });
    fabricRef.current.add(rect);
    fabricRef.current.setActiveObject(rect);
  };

  const addCircle = () => {
    if (!fabricRef.current) return;
    const circle = new fabric.Circle({
      left: 100,
      top: 100,
      fill: 'rgba(255,255,255,0.5)',
      radius: 60,
      stroke: '#ffffff',
      strokeWidth: 2,
      data: { id: `circle-${Date.now()}` }
    });
    fabricRef.current.add(circle);
    fabricRef.current.setActiveObject(circle);
  };

  const deleteSelected = () => {
    if (!fabricRef.current) return;
    const activeObjects = fabricRef.current.getActiveObjects();
    fabricRef.current.discardActiveObject();
    fabricRef.current.remove(...activeObjects);
  };

  const flipHorizontal = () => {
    if (!fabricRef.current) return;
    const activeObject = fabricRef.current.getActiveObject();
    if (activeObject) {
      activeObject.set('flipX', !activeObject.flipX);
      fabricRef.current.renderAll();
    }
  };

  const flipVertical = () => {
    if (!fabricRef.current) return;
    const activeObject = fabricRef.current.getActiveObject();
    if (activeObject) {
      activeObject.set('flipY', !activeObject.flipY);
      fabricRef.current.renderAll();
    }
  };

  const rotate = () => {
    if (!fabricRef.current) return;
    const activeObject = fabricRef.current.getActiveObject();
    if (activeObject) {
      activeObject.set('angle', (activeObject.angle || 0) + 90);
      fabricRef.current.renderAll();
    }
  };

  const animateZoom = (targetZoom: number, focalPoint?: fabric.Point) => {
    if (!fabricRef.current) return;
    const canvas = fabricRef.current;
    const startZoom = canvas.getZoom();
    const duration = 300;
    const startTime = Date.now();
    
    const finalZoom = Math.max(0.05, Math.min(20, targetZoom));

    // Determine the point to zoom into
    let point = focalPoint;
    if (!point) {
      const activeObject = canvas.getActiveObject();
      if (activeObject) {
        // If there's an active object, zoom towards its center
        const objectCenter = activeObject.getCenterPoint();
        const vpt = canvas.viewportTransform!;
        point = new fabric.Point(
          objectCenter.x * vpt[0] + vpt[4],
          objectCenter.y * vpt[3] + vpt[5]
        );
      } else {
        // Otherwise zoom towards the center of the viewport
        point = new fabric.Point(canvas.getWidth() / 2, canvas.getHeight() / 2);
      }
    }

    const performAnimation = () => {
      const currentTime = Date.now();
      const timeElapsed = currentTime - startTime;
      const progress = Math.min(timeElapsed / duration, 1);
      
      // easeInOutCubic
      const easedProgress = progress < 0.5
        ? 4 * progress * progress * progress
        : 1 - Math.pow(-2 * progress + 2, 3) / 2;
      
      const currentZoomValue = startZoom + (finalZoom - startZoom) * easedProgress;
      
      if (point) {
        canvas.zoomToPoint(point, currentZoomValue);
      }
      setCurrentZoom(currentZoomValue);
      
      if (progress < 1) {
        requestAnimationFrame(performAnimation);
      }
    };
    
    requestAnimationFrame(performAnimation);
  };

  const applyShadow = (settings = shadowSettings) => {
    if (!fabricRef.current) return;
    const activeObject = fabricRef.current.getActiveObject();
    if (activeObject) {
      const shadow = new fabric.Shadow({
        color: settings.color,
        blur: settings.blur,
        offsetX: settings.offsetX,
        offsetY: settings.offsetY,
        nonScaling: true
      });
      activeObject.set('shadow', shadow);
      fabricRef.current.renderAll();
      setLayers([...fabricRef.current.getObjects()].reverse());
    }
  };

  const removeShadow = () => {
    if (!fabricRef.current) return;
    const activeObject = fabricRef.current.getActiveObject();
    if (activeObject) {
      activeObject.set('shadow', null);
      fabricRef.current.renderAll();
      setLayers([...fabricRef.current.getObjects()].reverse());
    }
  };

  const [updateTrigger, setUpdateTrigger] = useState(0);
  const forceUpdate = () => setUpdateTrigger(t => t + 1);

  const updateTextProperty = (property: string, value: any) => {
    if (!fabricRef.current) return;
    const activeObject = fabricRef.current.getActiveObject();
    if (activeObject && (activeObject instanceof fabric.IText || activeObject instanceof fabric.Text)) {
      activeObject.set(property as any, value);
      fabricRef.current.requestRenderAll();
      forceUpdate();
    }
  };

  const allTools: Record<string, ToolConfig> = {
    undo: { id: 'undo', icon: <Undo2 size={16} />, label: t.undo || 'Undo', type: 'action', action: undo, disabled: historyIndex <= 0 },
    redo: { id: 'redo', icon: <Redo2 size={16} />, label: t.redo || 'Redo', type: 'action', action: redo, disabled: historyIndex >= history.length - 1 },
    select: { id: 'select', icon: <MousePointer2 size={16} />, label: t.select || 'Select', type: 'toggle', active: activeTool === 'select', action: () => setActiveTool('select') },
    text: { id: 'text', icon: <Type size={16} />, label: t.text || 'Add Text', type: 'action', action: addText },
    rect: { id: 'rect', icon: <Square size={16} />, label: t.rect || 'Add Square', type: 'action', action: addRect },
    circle: { id: 'circle', icon: <Circle size={16} />, label: t.circle || 'Add Circle', type: 'action', action: addCircle },
    crop: { id: 'crop', icon: <Crop size={16} />, label: t.crop || 'Crop', type: 'toggle', active: activeTool === 'crop', action: () => setActiveTool('crop') },
    layers: { id: 'layers', icon: <Layers size={16} />, label: t.layers || 'Layers', type: 'toggle', active: showLayers, action: () => setShowLayers(!showLayers) },
    flipH: { id: 'flipH', icon: <FlipHorizontal size={16} />, label: t.flipH || 'Flip H', type: 'action', action: flipHorizontal },
    flipV: { id: 'flipV', icon: <FlipVertical size={16} />, label: t.flipV || 'Flip V', type: 'action', action: flipVertical },
    rotate: { id: 'rotate', icon: <RotateCw size={16} />, label: t.rotate || 'Rotate', type: 'action', action: rotate },
    distort: { id: 'distort', icon: <Maximize2 size={16} className="rotate-12" />, label: t.adjustments || 'Distort', type: 'toggle', active: activeTool === 'distort', action: () => setActiveTool('distort') },
    shadow: { id: 'shadow', icon: <BoxSelect size={16} />, label: t.shadow || 'Shadow', type: 'toggle', active: activeTool === 'shadow', action: () => setActiveTool('shadow') },
    delete: { id: 'delete', icon: <Trash2 size={16} className="text-red-500" />, label: t.delete || 'Delete', type: 'action', action: deleteSelected },
  };

  const moveTool = (index: number, direction: 'up' | 'down') => {
    const newItems = [...toolbarItems];
    const newIndex = direction === 'up' ? index - 1 : index + 1;
    if (newIndex >= 0 && newIndex < newItems.length) {
      [newItems[index], newItems[newIndex]] = [newItems[newIndex], newItems[index]];
      setToolbarItems(newItems);
    }
  };

  const toggleToolVisibility = (itemId: string) => {
    setHiddenTools(prev => 
      prev.includes(itemId) 
        ? prev.filter(id => id !== itemId) 
        : [...prev, itemId]
    );
  };

  const addSpacer = () => {
    const spacerId = `spacer-${Date.now()}`;
    setToolbarItems(prev => [...prev, spacerId]);
  };

  const removeSpacer = (id: string) => {
    setToolbarItems(prev => prev.filter(item => item !== id));
  };

  const handleReorder = (newOrder: fabric.Object[]) => {
    if (!fabricRef.current) return;
    const canvas = fabricRef.current;
    
    // Calculate new Fabric indices
    // newOrder[0] is topmost, so it should have index = total - 1
    // newOrder[last] is bottommost, so it should have index = 0
    const total = newOrder.length;
    
    newOrder.forEach((obj, i) => {
      const fabricIndex = total - 1 - i;
      (obj as any).moveTo(fabricIndex);
    });
    
    canvas.requestRenderAll();
    setLayers(newOrder);
  };

  const handleFontUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    // Support .ttf, .otf, .woff, .woff2
    const fontName = file.name.split('.')[0].replace(/[^a-zA-Z0-9]/g, '');
    const reader = new FileReader();

    reader.onload = async (event) => {
      const arrayBuffer = event.target?.result as ArrayBuffer;
      try {
        const fontFace = new FontFace(fontName, arrayBuffer);
        await fontFace.load();
        (document.fonts as any).add(fontFace);
        
        setCustomFonts(prev => [...prev, fontName]);
        
        // If an object is selected, apply the font immediately
        if (fabricRef.current) {
          const activeObject = fabricRef.current.getActiveObject();
          if (activeObject && (activeObject instanceof fabric.IText || activeObject instanceof fabric.Text)) {
            activeObject.set('fontFamily', fontName);
            fabricRef.current.requestRenderAll();
            forceUpdate();
          }
        }
      } catch (err) {
        console.error('Failed to load font:', err);
      }
    };
    reader.readAsArrayBuffer(file);
  };

  return (
    <div className="relative flex-1 w-full flex flex-col items-center justify-center p-4 md:p-12 touch-none overflow-hidden min-h-0">
      {/* Export Format Selector Modal */}
      {showExportModal && (
        <div className="absolute inset-0 z-[110] glass flex items-center justify-center p-8 animate-in fade-in duration-300">
          <motion.div 
            initial={{ scale: 0.9, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            className="w-full max-w-sm glass bg-black/60 border border-white/10 rounded-[2rem] p-8 space-y-8 shadow-2xl"
          >
            <div className="text-center space-y-2">
              <h3 className="text-2xl font-bold tracking-tight text-white">{t.exportAs}</h3>
              <p className="text-white/40 text-sm italic">{t.format}</p>
            </div>

            <div className="grid grid-cols-2 gap-4">
              {['png', 'jpeg'].map((format) => (
                <button
                  key={format}
                  onClick={() => setSelectedFormat(format as any)}
                  className={cn(
                    "p-6 rounded-2xl border-2 transition-all flex flex-col items-center gap-3",
                    selectedFormat === format ? "bg-ios-blue/20 border-ios-blue shadow-lg shadow-blue-500/20" : "bg-white/5 border-transparent hover:bg-white/10"
                  )}
                >
                  <div className={cn(
                    "w-12 h-12 rounded-xl flex items-center justify-center shadow-inner",
                    selectedFormat === format ? "bg-ios-blue text-white" : "bg-white/5 text-white/40"
                  )}>
                    <ImageIcon size={24} />
                  </div>
                  <span className={cn(
                    "text-[10px] font-bold uppercase tracking-[0.2em]",
                    selectedFormat === format ? "text-ios-blue border-b border-ios-blue/30" : "text-white/40"
                  )}>
                    {format}
                  </span>
                </button>
              ))}
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

      {/* Incompatible File Warning */}
      {isVideoInPhoto && (
        <div className="absolute inset-0 z-50 glass flex items-center justify-center p-12 text-center flex-col gap-6 animate-in fade-in zoom-in duration-500">
          <div className="w-24 h-24 rounded-3xl bg-white/5 flex items-center justify-center text-ios-blue shadow-2xl relative">
             <div className="absolute inset-0 bg-ios-blue/20 blur-2xl rounded-full" />
             <ImageIcon size={48} className="relative z-10" />
          </div>
          <div className="max-w-md space-y-2">
            <h3 className="text-2xl font-bold tracking-tight">{t.incompatibleFile}</h3>
            <p className="text-white/40 text-sm font-medium leading-relaxed">
              {t.uploadVideoPrompt.replace(t.video.toLowerCase(), t.photo.toLowerCase())}
            </p>
          </div>
          <label className="bg-ios-blue px-8 py-3 rounded-full text-sm font-bold hover:bg-ios-blue/80 transition-all cursor-pointer active:scale-95 shadow-xl shadow-blue-500/20 uppercase tracking-widest">
            {t.select} {t.photo}
            <input 
              type="file" 
              accept="image/*" 
              className="hidden" 
              onChange={(e) => {
                const file = e.target.files?.[0];
                if (file) {
                  window.dispatchEvent(new CustomEvent('app-file-change', { detail: file }));
                }
              }} 
            />
          </label>
        </div>
      )}

      {/* Tool Overlay - Frosted Glass */}
      <div className="absolute top-8 left-1/2 -translate-x-1/2 z-20 flex gap-1.5 p-1.5 glass rounded-3xl ios-shadow scale-110">
        {toolbarItems.map((itemId) => {
          if (hiddenTools.includes(itemId)) return null;
          if (itemId.startsWith('spacer')) {
            return <div key={itemId} className="w-[1px] h-4 bg-white/10 mx-1 self-center" />;
          }
          const tool = allTools[itemId];
          if (!tool) return null;
          return (
            <ToolButton
              key={tool.id}
              icon={tool.icon}
              active={tool.active}
              disabled={tool.disabled}
              onClick={tool.action || (() => {})}
              label={tool.label}
            />
          );
        })}
        <div className="w-[1px] h-4 bg-white/10 mx-1 self-center" />
        <ToolButton
          icon={<Settings2 size={16} className="text-ios-blue" />}
          onClick={() => setIsCustomizingToolbar(true)}
          label="Customize Toolbar"
        />
      </div>

      <AnimatePresence>
        {isCustomizingToolbar && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-[100] flex items-center justify-center bg-black/60 backdrop-blur-sm p-6"
          >
            <motion.div
              initial={{ scale: 0.9, opacity: 0, y: 20 }}
              animate={{ scale: 1, opacity: 1, y: 0 }}
              exit={{ scale: 0.9, opacity: 0, y: 20 }}
              className="bg-zinc-900 border border-white/10 rounded-[32px] w-full max-w-lg overflow-hidden flex flex-col max-h-[80vh] ios-shadow"
            >
              <div className="p-6 border-b border-white/5 flex items-center justify-between bg-white/5">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-2xl bg-ios-blue/20 flex items-center justify-center">
                    <Settings2 size={20} className="text-ios-blue" />
                  </div>
                  <div>
                    <h3 className="text-lg font-bold text-white tracking-tight">Customize Toolbar</h3>
                    <p className="text-[10px] text-white/40 font-bold uppercase tracking-widest">Reorder or toggle tools</p>
                  </div>
                </div>
                <button 
                  onClick={() => setIsCustomizingToolbar(false)}
                  className="w-10 h-10 rounded-full hover:bg-white/10 flex items-center justify-center transition-colors"
                >
                  <X size={20} className="text-white/60" />
                </button>
              </div>

              <div className="flex-1 overflow-y-auto p-4 space-y-2">
                {toolbarItems.map((itemId, index) => {
                  const isSpacer = itemId.startsWith('spacer');
                  const tool = allTools[itemId];
                  const isHidden = hiddenTools.includes(itemId);

                  return (
                    <div 
                      key={itemId}
                      className={cn(
                        "flex items-center gap-4 p-3 rounded-2xl transition-all border",
                        isHidden ? "opacity-40 bg-transparent border-transparent" : "bg-white/5 border-white/5 shadow-sm"
                      )}
                    >
                      <div className="flex flex-col gap-1">
                        <button 
                          onClick={() => moveTool(index, 'up')}
                          disabled={index === 0}
                          className="p-1 hover:text-ios-blue disabled:opacity-0"
                        >
                          <ChevronUp size={14} />
                        </button>
                        <button 
                          onClick={() => moveTool(index, 'down')}
                          disabled={index === toolbarItems.length - 1}
                          className="p-1 hover:text-ios-blue disabled:opacity-0"
                        >
                          <ChevronDown size={14} />
                        </button>
                      </div>

                      <div className="w-10 h-10 rounded-xl bg-white/5 flex items-center justify-center text-white/60">
                        {isSpacer ? <GripVertical size={16} /> : tool?.icon}
                      </div>

                      <div className="flex-1">
                        <p className={cn(
                          "text-xs font-bold tracking-tight",
                          isHidden ? "text-white/40" : "text-white"
                        )}>
                          {isSpacer ? 'Vertical Spacer' : tool?.label}
                        </p>
                        {isSpacer && (
                          <p className="text-[9px] text-white/20 font-bold uppercase tracking-widest">Layout Element</p>
                        )}
                      </div>

                      <div className="flex items-center gap-2">
                        {isSpacer ? (
                          <button 
                            onClick={() => removeSpacer(itemId)}
                            className="p-2 rounded-xl bg-red-500/10 text-red-500 hover:bg-red-500"
                          >
                            <Trash2 size={16} />
                          </button>
                        ) : (
                          <button 
                            onClick={() => toggleToolVisibility(itemId)}
                            className={cn(
                              "px-3 py-1.5 rounded-xl text-[10px] font-bold uppercase tracking-widest transition-all",
                              isHidden 
                                ? "bg-white/5 text-white/40 hover:bg-white/10" 
                                : "bg-ios-blue/10 text-ios-blue hover:bg-ios-blue/20"
                            )}
                          >
                            {isHidden ? 'Show' : 'Hide'}
                          </button>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>

              <div className="p-4 border-t border-white/5 bg-white/5 flex gap-3">
                <button 
                  onClick={addSpacer}
                  className="flex-1 flex items-center justify-center gap-2 py-3 rounded-2xl glass text-[10px] font-bold uppercase tracking-widest hover:bg-white/10 transition-all"
                >
                  <Plus size={14} className="text-ios-blue" /> Add Spacer
                </button>
                <button 
                  onClick={() => {
                    setToolbarItems([
                      'undo', 'redo', 'spacer-1', 'select', 'text', 'rect', 'circle', 'distort', 'spacer-2', 
                      'crop', 'spacer-3', 'layers', 'spacer-4', 'flipH', 'flipV', 'rotate', 'shadow', 
                      'spacer-5', 'delete'
                    ]);
                    setHiddenTools([]);
                  }}
                  className="flex-1 flex items-center justify-center gap-2 py-3 rounded-2xl glass text-[10px] font-bold uppercase tracking-widest hover:text-red-500 transition-all"
                >
                   Reset Defaults
                </button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      <div ref={containerRef} className="w-full h-full max-w-6xl rounded-3xl overflow-hidden ios-shadow bg-zinc-900 border border-white/5 relative">
        <canvas ref={canvasRef} />
        
        {/* Zoom Controls */}
        <div className="absolute bottom-6 right-6 z-40 flex flex-col gap-3">
          <div className="glass p-1 rounded-2xl ios-shadow flex flex-col gap-1 border border-white/5">
            <ToolButton 
              icon={<ZoomIn size={14} />} 
              onClick={() => animateZoom(fabricRef.current ? fabricRef.current.getZoom() * 1.5 : 1.5)} 
              label="Zoom In"
            />
            <ToolButton 
              icon={<ZoomOut size={14} />} 
              onClick={() => animateZoom(fabricRef.current ? fabricRef.current.getZoom() / 1.5 : 0.75)} 
              label="Zoom Out"
            />
            <div className="w-4 h-[1px] bg-white/10 mx-auto my-1" />
            <ToolButton 
              icon={<Maximize2 size={14} />} 
              onClick={() => {
                if (!fabricRef.current) return;
                const canvas = fabricRef.current;
                canvas.setViewportTransform([1, 0, 0, 1, 0, 0]);
                canvas.setZoom(1);
                setCurrentZoom(1);
                canvas.renderAll();
              }} 
              label="Reset View" 
            />
          </div>
          <div className="glass px-2 py-1.5 rounded-xl text-[9px] font-mono font-bold text-ios-blue text-center border border-white/5 ios-shadow">
            {Math.round(currentZoom * 100)}%
          </div>
        </div>
        
        {/* Distort Controls */}
        <AnimatePresence>
          {activeTool === 'distort' && (
            <motion.div
              initial={{ y: 50, opacity: 0 }}
              animate={{ y: 0, opacity: 1 }}
              exit={{ y: 50, opacity: 0 }}
              className="absolute bottom-10 left-1/2 -translate-x-1/2 glass rounded-3xl p-6 ios-shadow flex flex-col gap-6 z-40 w-[400px]"
            >
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <Maximize2 size={16} className="text-ios-blue rotate-12" />
                  <span className="text-[10px] font-bold tracking-[0.2em] uppercase opacity-40">Distortion</span>
                </div>
                <button 
                  onClick={() => setActiveTool('select')}
                  className="p-1.5 hover:bg-white/10 rounded-full transition-colors"
                >
                  <X size={14} />
                </button>
              </div>

              <div className="grid grid-cols-2 gap-8">
                {/* Skew X */}
                <div className="flex flex-col gap-3">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                       <MoveHorizontal size={12} className="text-ios-blue" />
                       <span className="text-[9px] font-bold tracking-widest text-white/30 uppercase">{t.skewX}</span>
                    </div>
                    <span className="text-[9px] font-mono text-ios-blue">{filters.skewX}°</span>
                  </div>
                  <input 
                    type="range" min="-45" max="45" step="1" 
                    value={filters.skewX} 
                    onChange={(e) => onUpdateFilters({ ...filters, skewX: parseInt(e.target.value) })}
                    className="w-full h-1 bg-white/10 rounded-full accent-ios-blue appearance-none cursor-pointer"
                  />
                </div>

                {/* Skew Y */}
                <div className="flex flex-col gap-3">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                       <MoveVertical size={12} className="text-ios-blue" />
                       <span className="text-[9px] font-bold tracking-widest text-white/30 uppercase">{t.skewY}</span>
                    </div>
                    <span className="text-[9px] font-mono text-ios-blue">{filters.skewY}°</span>
                  </div>
                  <input 
                    type="range" min="-45" max="45" step="1" 
                    value={filters.skewY} 
                    onChange={(e) => onUpdateFilters({ ...filters, skewY: parseInt(e.target.value) })}
                    className="w-full h-1 bg-white/10 rounded-full accent-ios-blue appearance-none cursor-pointer"
                  />
                </div>
              </div>

              <div className="flex gap-2">
                 <button 
                  onClick={() => onUpdateFilters({ ...filters, skewX: 0, skewY: 0 })}
                  className="flex-1 py-3 bg-white/5 text-white/40 rounded-2xl font-bold text-[10px] tracking-widest uppercase hover:bg-white/10 hover:text-white transition-all"
                >
                  {t.resetAll}
                </button>
                <button 
                  onClick={() => setActiveTool('select')}
                  className="flex-1 py-3 bg-ios-blue text-white rounded-2xl font-bold text-[10px] tracking-widest uppercase hover:opacity-90 transition-all shadow-xl shadow-blue-500/20"
                >
                  {t.apply}
                </button>
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        {/* Text Controls */}
        <AnimatePresence>
          {(selectedObject instanceof fabric.IText || selectedObject instanceof fabric.Text) && activeTool !== 'crop' && (
            <motion.div
              drag
              dragMomentum={false}
              initial={{ scale: 0.9, opacity: 0, x: "-50%", y: "-40%" }}
              animate={{ scale: 1, opacity: 1, x: "-50%", y: "-50%" }}
              exit={{ scale: 0.9, opacity: 0, x: "-50%", y: "-40%" }}
              className="absolute top-1/2 left-1/2 glass rounded-3xl p-6 ios-shadow flex flex-col gap-5 z-[60] w-[420px] border border-white/10"
            >
              <div className="flex items-center justify-between pb-2 border-b border-white/5">
                <div className="flex items-center gap-3 cursor-move">
                  <GripVertical size={14} className="text-white/20" />
                  <Type size={16} className="text-ios-blue" />
                  <span className="text-[10px] font-bold tracking-[0.2em] uppercase opacity-40">{t.text}</span>
                </div>
                <button 
                  onClick={() => {
                    fabricRef.current?.discardActiveObject();
                    fabricRef.current?.requestRenderAll();
                  }}
                  className="p-1.5 hover:bg-white/10 rounded-full transition-colors"
                >
                  <X size={14} />
                </button>
              </div>

              <div className="flex flex-col gap-5">
                {/* Text Content Editor */}
                <div className="flex flex-col gap-2">
                  <span className="text-[9px] font-bold tracking-widest text-white/30 uppercase">{t.editText}</span>
                  <textarea 
                    value={(selectedObject as fabric.IText).text}
                    onChange={(e) => updateTextProperty('text', e.target.value)}
                    className="w-full bg-white/5 border border-white/10 rounded-2xl px-4 py-3 text-xs text-white outline-none focus:border-ios-blue transition-all resize-none h-20 font-medium"
                    placeholder={t.enterText}
                  />
                </div>

                {/* Font Family */}
                <div className="flex flex-col gap-2">
                  <div className="flex items-center justify-between">
                    <span className="text-[9px] font-bold tracking-widest text-white/30 uppercase">{t.font}</span>
                    <button 
                      onClick={() => fontInputRef.current?.click()}
                      className="flex items-center gap-1.5 px-2 py-1 rounded-lg hover:bg-white/5 text-ios-blue transition-all"
                    >
                      <Upload size={10} />
                      <span className="text-[8px] font-bold uppercase tracking-wider">{t.uploadFont}</span>
                    </button>
                    <input 
                      type="file" 
                      ref={fontInputRef} 
                      className="hidden" 
                      accept=".ttf,.otf,.woff,.woff2" 
                      onChange={handleFontUpload}
                    />
                  </div>
                  <div className="flex gap-1.5 glass p-1 rounded-2xl overflow-x-auto custom-scrollbar">
                    {[
                      { name: 'Inter', display: 'Inter' },
                      { name: 'Space Grotesk', display: 'Space' },
                      { name: 'Outfit', display: 'Outfit' },
                      { name: 'JetBrains Mono', display: 'Mono' },
                      ...customFonts.map(f => ({ name: f, display: f.slice(0, 8) + (f.length > 8 ? '..' : '') }))
                    ].map(font => (
                      <button
                        key={font.name}
                        onClick={() => updateTextProperty('fontFamily', font.name)}
                        className={cn(
                          "flex-1 px-3 py-2 rounded-xl text-[10px] font-bold transition-all whitespace-nowrap min-w-[60px]",
                          (selectedObject as fabric.IText).fontFamily === font.name 
                            ? "bg-white text-black shadow-lg scale-[1.02]" 
                            : "text-white/40 hover:bg-white/5 hover:text-white"
                        )}
                        style={{ fontFamily: font.name }}
                      >
                        {font.display}
                      </button>
                    ))}
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-6">
                  {/* Font Size */}
                  <div className="flex flex-col gap-2">
                    <div className="flex items-center justify-between">
                      <span className="text-[9px] font-bold tracking-widest text-white/30 uppercase">{t.fontSize}</span>
                      <span className="text-[9px] font-mono text-ios-blue">{Math.round((selectedObject as fabric.IText).fontSize || 0)}px</span>
                    </div>
                    <input 
                      type="range" min="8" max="300" step="1" 
                      value={(selectedObject as fabric.IText).fontSize} 
                      onChange={(e) => updateTextProperty('fontSize', parseInt(e.target.value))}
                      className="w-full h-1 bg-white/10 rounded-full accent-ios-blue appearance-none cursor-pointer"
                    />
                  </div>

                  {/* Text Alignment */}
                  <div className="flex flex-col gap-2">
                    <span className="text-[9px] font-bold tracking-widest text-white/30 uppercase">{t.alignment}</span>
                    <div className="flex gap-1 p-1 glass rounded-xl h-[32px]">
                      {[
                        { id: 'left', label: 'L' },
                        { id: 'center', label: 'C' },
                        { id: 'right', label: 'R' }
                      ].map((align) => (
                        <button
                          key={align.id}
                          onClick={() => updateTextProperty('textAlign', align.id)}
                          className={cn(
                            "flex-1 rounded-lg text-[9px] font-bold uppercase transition-all",
                            (selectedObject as fabric.IText).textAlign === align.id ? "bg-white text-black shadow-sm" : "text-white/40 hover:text-white"
                          )}
                        >
                          {align.label}
                        </button>
                      ))}
                    </div>
                  </div>
                </div>

                {/* Color and Custom Picker */}
                <div className="flex flex-col gap-2">
                  <div className="flex items-center justify-between">
                    <span className="text-[9px] font-bold tracking-widest text-white/30 uppercase">{t.color}</span>
                    <div className="flex items-center gap-2">
                       <span className="text-[9px] font-mono text-white/40 uppercase">{(selectedObject as fabric.IText).fill}</span>
                       <input 
                        type="color" 
                        value={(selectedObject as fabric.IText).fill as string}
                        onChange={(e) => updateTextProperty('fill', e.target.value)}
                        className="w-6 h-6 rounded-full bg-transparent border-none p-0 cursor-pointer overflow-hidden ring-1 ring-white/10"
                      />
                    </div>
                  </div>
                  <div className="flex gap-2.5 items-center flex-wrap">
                    {['#ffffff', '#000000', '#ff3b30', '#ff9500', '#ffcc00', '#34c759', '#007aff', '#5856d6', '#af52de', '#ff2d55'].map(c => (
                      <button
                        key={c}
                        onClick={() => updateTextProperty('fill', c)}
                        className={cn(
                          "w-6 h-6 rounded-full border border-white/20 transition-all shadow-sm",
                          (selectedObject as fabric.IText).fill === c ? "scale-125 shadow-lg ring-2 ring-ios-blue border-white" : "hover:scale-110"
                        )}
                        style={{ backgroundColor: c }}
                      />
                    ))}
                  </div>
                </div>
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        {/* Shadow Controls */}
        <AnimatePresence>
          {activeTool === 'shadow' && (
            <motion.div
              initial={{ y: 50, opacity: 0 }}
              animate={{ y: 0, opacity: 1 }}
              exit={{ y: 50, opacity: 0 }}
              className="absolute bottom-10 left-1/2 -translate-x-1/2 glass rounded-3xl p-6 ios-shadow flex flex-col gap-6 z-40 w-[400px]"
            >
              <div className="flex items-center justify-between">
                <span className="text-[10px] font-bold tracking-[0.2em] uppercase opacity-40">{t.shadow}</span>
                <button 
                  onClick={() => setActiveTool('select')}
                  className="p-1.5 hover:bg-white/10 rounded-full transition-colors"
                >
                  <X size={14} />
                </button>
              </div>

              <div className="grid grid-cols-2 gap-x-8 gap-y-6">
                {/* Color */}
                <div className="flex flex-col gap-2">
                  <span className="text-[9px] font-bold tracking-widest text-white/30 uppercase">Color</span>
                  <div className="flex gap-2">
                    {['#000000', '#ffffff', '#ff3b30', '#4cd964', '#007aff'].map(c => (
                      <button
                        key={c}
                        onClick={() => {
                          const newSettings = { ...shadowSettings, color: c };
                          setShadowSettings(newSettings);
                          applyShadow(newSettings);
                        }}
                        className={cn(
                          "w-6 h-6 rounded-full border border-white/20 transition-all",
                          shadowSettings.color === c ? "scale-110 shadow-lg" : "hover:scale-105"
                        )}
                        style={{ backgroundColor: c }}
                      />
                    ))}
                  </div>
                </div>

                {/* Blur */}
                <div className="flex flex-col gap-2">
                  <div className="flex items-center justify-between">
                    <span className="text-[9px] font-bold tracking-widest text-white/30 uppercase">Blur</span>
                    <span className="text-[9px] font-mono text-ios-blue">{shadowSettings.blur}px</span>
                  </div>
                  <input 
                    type="range" min="0" max="50" step="1" 
                    value={shadowSettings.blur} 
                    onChange={(e) => {
                      const newSettings = { ...shadowSettings, blur: parseInt(e.target.value) };
                      setShadowSettings(newSettings);
                      applyShadow(newSettings);
                    }}
                    className="w-full h-1 bg-white/10 rounded-full accent-ios-blue appearance-none"
                  />
                </div>

                {/* Offset X */}
                <div className="flex flex-col gap-2">
                  <div className="flex items-center justify-between">
                    <span className="text-[9px] font-bold tracking-widest text-white/30 uppercase">Offset X</span>
                    <span className="text-[9px] font-mono text-ios-blue">{shadowSettings.offsetX}px</span>
                  </div>
                  <input 
                    type="range" min="-30" max="30" step="1" 
                    value={shadowSettings.offsetX} 
                    onChange={(e) => {
                      const newSettings = { ...shadowSettings, offsetX: parseInt(e.target.value) };
                      setShadowSettings(newSettings);
                      applyShadow(newSettings);
                    }}
                    className="w-full h-1 bg-white/10 rounded-full accent-ios-blue appearance-none"
                  />
                </div>

                {/* Offset Y */}
                <div className="flex flex-col gap-2">
                  <div className="flex items-center justify-between">
                    <span className="text-[9px] font-bold tracking-widest text-white/30 uppercase">Offset Y</span>
                    <span className="text-[9px] font-mono text-ios-blue">{shadowSettings.offsetY}px</span>
                  </div>
                  <input 
                    type="range" min="-30" max="30" step="1" 
                    value={shadowSettings.offsetY} 
                    onChange={(e) => {
                      const newSettings = { ...shadowSettings, offsetY: parseInt(e.target.value) };
                      setShadowSettings(newSettings);
                      applyShadow(newSettings);
                    }}
                    className="w-full h-1 bg-white/10 rounded-full accent-ios-blue appearance-none"
                  />
                </div>
              </div>

              <div className="flex gap-2 mt-2">
                <button 
                  onClick={() => applyShadow()}
                  className="flex-1 py-3 bg-ios-blue text-white rounded-2xl font-bold text-[10px] tracking-widest uppercase hover:opacity-90 transition-all shadow-xl shadow-blue-500/20"
                >
                  {t.apply}
                </button>
                <button 
                  onClick={removeShadow}
                  className="px-6 py-3 bg-white/5 text-white/40 rounded-2xl font-bold text-[10px] tracking-widest uppercase hover:bg-white/10 hover:text-white transition-all"
                >
                  {t.cancel}
                </button>
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        {/* Crop Confirmation Overlay */}
        {activeTool === 'crop' && (
          <div className="absolute bottom-10 left-1/2 -translate-x-1/2 z-30 flex flex-col items-center gap-4">
            <div className="flex gap-2 glass p-1.5 rounded-2xl">
              {[
                { label: 'Free', ratio: null },
                { label: '1:1', ratio: 1 },
                { label: '16:9', ratio: 16/9 },
                { label: '4:3', ratio: 4/3 },
                { label: '9:16', ratio: 9/16 }
              ].map((r) => (
                <button
                  key={r.label}
                  onClick={() => setCropAspectRatio(r.ratio)}
                  className="px-3 py-1.5 rounded-xl text-[9px] font-bold tracking-widest uppercase hover:bg-white/10 transition-all text-white/60 hover:text-white"
                >
                  {r.label}
                </button>
              ))}
            </div>
            <button 
              onClick={applyCrop}
              className="px-6 py-3 bg-ios-blue text-white rounded-full font-bold shadow-xl shadow-blue-500/30 hover:scale-105 transition-all text-xs tracking-widest uppercase"
            >
              Apply Crop Selection
            </button>
          </div>
        )}

        {/* Layers Panel */}
        <AnimatePresence>
          {showLayers && (
            <motion.div
              initial={{ x: 300, opacity: 0 }}
              animate={{ x: 0, opacity: 1 }}
              exit={{ x: 300, opacity: 0 }}
              className="absolute right-6 top-6 bottom-6 w-64 glass rounded-3xl p-4 flex flex-col gap-4 z-30"
            >
              <div className="flex items-center justify-between opacity-40 px-2">
                <span className="text-[10px] font-bold tracking-[0.2em] uppercase">{t.layers}</span>
                <button onClick={() => setShowLayers(false)}><X size={14} /></button>
              </div>
              
              <div className="flex-1 overflow-hidden">
                <div className="flex gap-2 mb-2 px-1">
                  <button 
                    onClick={() => {
                      const canvas = fabricRef.current;
                      if (!canvas) return;
                      const activeObject = canvas.getActiveObject();
                      if (activeObject && activeObject.type === 'activeSelection') {
                        (activeObject as any).toGroup();
                        canvas.requestRenderAll();
                        setLayers([...canvas.getObjects()].reverse());
                      }
                    }}
                    className="flex-1 py-2 glass rounded-xl text-[9px] font-bold tracking-widest uppercase hover:bg-ios-blue hover:text-white transition-all disabled:opacity-30"
                    disabled={fabricRef.current?.getActiveObject()?.type !== 'activeSelection'}
                  >
                    Group
                  </button>
                  <button 
                    onClick={() => {
                      const canvas = fabricRef.current;
                      if (!canvas) return;
                      const activeObject = canvas.getActiveObject();
                      if (activeObject instanceof fabric.Group) {
                        (activeObject as any).toActiveSelection();
                        canvas.requestRenderAll();
                        setLayers([...canvas.getObjects()].reverse());
                      }
                    }}
                    className="flex-1 py-2 glass rounded-xl text-[9px] font-bold tracking-widest uppercase hover:bg-red-500 hover:text-white transition-all disabled:opacity-30"
                    disabled={!(fabricRef.current?.getActiveObject() instanceof fabric.Group)}
                  >
                    Ungroup
                  </button>
                </div>

                <Reorder.Group 
                  axis="y" 
                  values={layers} 
                  onReorder={handleReorder}
                  className="flex-1 overflow-y-auto space-y-2 pr-1 h-full custom-scrollbar"
                >
                  {layers.map((obj) => (
                    <Reorder.Item 
                      key={obj.data?.id || layers.indexOf(obj)} 
                      value={obj}
                      initial={{ opacity: 0, x: 20 }}
                      animate={{ opacity: 1, x: 0 }}
                      exit={{ opacity: 0, x: -20 }}
                    >
                      <LayerItem 
                        obj={obj} 
                        isActive={fabricRef.current?.getActiveObject() === obj}
                        onSelect={() => {
                          fabricRef.current?.setActiveObject(obj);
                          fabricRef.current?.requestRenderAll();
                        }}
                        onDelete={() => {
                          fabricRef.current?.remove(obj);
                          fabricRef.current?.requestRenderAll();
                        }}
                        onMoveUp={() => {
                          obj.bringForward();
                          fabricRef.current?.requestRenderAll();
                        }}
                        onMoveDown={() => {
                          obj.sendBackwards();
                          fabricRef.current?.requestRenderAll();
                        }}
                        onOpacityChange={(val: number) => {
                          obj.set('opacity', val);
                          fabricRef.current?.requestRenderAll();
                          // Force a re-render to update the slider text
                          setLayers([...fabricRef.current!.getObjects()].reverse());
                        }}
                        onToggleLock={() => {
                          const canvas = fabricRef.current;
                          if (!canvas) return;
                          const isLocked = !obj.selectable;
                          obj.set({
                            selectable: !isLocked,
                            evented: !isLocked,
                            lockMovementX: isLocked,
                            lockMovementY: isLocked,
                            lockScalingX: isLocked,
                            lockScalingY: isLocked,
                            lockRotation: isLocked,
                            hasControls: !isLocked,
                            hasBorders: !isLocked
                          });
                          canvas.discardActiveObject();
                          canvas.requestRenderAll();
                          setLayers([...canvas.getObjects()].reverse());
                        }}
                      />
                    </Reorder.Item>
                  ))}
                </Reorder.Group>
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </div>
  );
};

const LayerItem = ({ obj, isActive, onSelect, onDelete, onMoveUp, onMoveDown, onOpacityChange, onToggleLock }: any) => {
  const isImage = obj instanceof fabric.FabricImage;
  const isText = obj instanceof fabric.IText;
  const isGroup = obj instanceof fabric.Group;
  const isLocked = !obj.selectable;
  
  return (
    <div 
      onClick={!isLocked ? onSelect : undefined}
      className={cn(
        "p-3 rounded-2xl flex flex-col gap-2 transition-all border",
        isActive 
          ? "bg-white/15 border-ios-blue shadow-lg scale-[1.02] z-10" 
          : "bg-white/5 border-white/5 hover:bg-white/10",
        isLocked ? "cursor-not-allowed opacity-60" : "cursor-pointer"
      )}
    >
      <div className="flex items-center gap-3">
        <div className="w-8 h-8 rounded-lg bg-zinc-800 flex items-center justify-center text-white/40 ring-1 ring-white/5">
          {isImage ? <ImageIcon size={14} /> : isText ? <Type size={14} /> : isGroup ? <Layers size={14} /> : <Square size={14} />}
        </div>
        <div className="flex-1 overflow-hidden">
          <div className={cn(
            "text-[10px] font-bold truncate lowercase grayscale transition-all",
            isActive ? "text-ios-blue grayscale-0" : "text-white opacity-80"
          )}>
            {isText ? obj.text : isImage ? "Image Asset" : isGroup ? "Grouped Layer" : "Vector Shape"}
          </div>
          <div className="text-[8px] text-white/20 uppercase font-bold tracking-tighter">
            {isActive ? "Active Layer" : "Layer"}
          </div>
        </div>
        <div className="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
          <GripVertical size={14} className="text-white/20 mr-1 cursor-grab active:cursor-grabbing" />
          <button 
            onClick={(e) => { e.stopPropagation(); onToggleLock(); }} 
            className={cn(
              "p-1 transition-colors",
              isLocked ? "text-red-500" : "text-white/40 hover:text-ios-blue"
            )}
          >
            {isLocked ? <Lock size={14} /> : <LockOpen size={14} />}
          </button>
          {!isLocked && (
            <button onClick={(e) => { e.stopPropagation(); onDelete(); }} className="p-1 hover:text-red-500 text-white/40"><Trash2 size={14} /></button>
          )}
        </div>
      </div>
      
      {/* Opacity Slider */}
      <div className={cn(
        "px-1 flex items-center gap-3 transition-opacity",
        isActive ? "opacity-100" : "opacity-0 group-hover:opacity-100"
      )}>
        <span className="text-[8px] font-bold text-white/30 uppercase tracking-widest">Opacity</span>
        <input 
          type="range" 
          min="0" 
          max="1" 
          step="0.01" 
          value={obj.opacity ?? 1} 
          onChange={(e) => !isLocked && onOpacityChange(parseFloat(e.target.value))}
          onClick={(e) => e.stopPropagation()}
          disabled={isLocked}
          className={cn(
            "flex-1 h-1 bg-white/10 rounded-full appearance-none cursor-pointer accent-ios-blue",
            isLocked && "opacity-30 cursor-not-allowed"
          )}
        />
        <span className="text-[8px] font-mono text-white/40 w-6 text-right">
          {Math.round((obj.opacity ?? 1) * 100)}%
        </span>
      </div>
    </div>
  );
};

interface ToolButtonProps {
  icon: React.ReactNode;
  active?: boolean;
  onClick: () => void;
  label: string;
  disabled?: boolean;
}

const ToolButton: React.FC<ToolButtonProps> = ({ icon, active, onClick, label, disabled }) => (
  <button
    onClick={onClick}
    disabled={disabled}
    className={cn(
      "w-10 h-10 rounded-[18px] transition-all duration-300 flex items-center justify-center relative group",
      active ? "bg-white text-black" : "text-white/40 hover:bg-white/10 hover:text-white",
      "disabled:opacity-20 disabled:pointer-events-none"
    )}
  >
    {icon}
    <div className="absolute -top-12 left-1/2 -translate-x-1/2 px-2 py-1.5 glass rounded-xl text-[9px] font-bold tracking-[0.2em] opacity-0 group-hover:opacity-100 transition-all duration-300 whitespace-nowrap pointer-events-none z-50 border border-white/10 scale-90 group-hover:scale-100 uppercase">
      {label}
      <div className="absolute top-full left-1/2 -translate-x-1/2 border-x-4 border-x-transparent border-t-4 border-t-white/10" />
    </div>
  </button>
);

export default PhotoEditor;
