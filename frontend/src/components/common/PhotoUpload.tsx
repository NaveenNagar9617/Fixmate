import { useCallback, useState } from 'react';
import { useDropzone } from 'react-dropzone';
import { Upload, X, CheckCircle2, Loader2 } from 'lucide-react';
import { cn } from '@/lib/utils';

interface PhotoUploadProps {
  file: File | null;
  onFileChange: (file: File | null) => void;
  label?: string;
}

function formatBytes(bytes: number): string {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return `${(bytes / Math.pow(k, i)).toFixed(1)} ${sizes[i]}`;
}

async function compressImage(file: File): Promise<{ compressedFile: File; originalSize: number; compressedSize: number }> {
  return new Promise((resolve) => {
    // If not a standard image format, return as is
    if (!file.type.startsWith('image/')) {
      resolve({ compressedFile: file, originalSize: file.size, compressedSize: file.size });
      return;
    }

    const img = new Image();
    const reader = new FileReader();

    reader.onload = (e) => {
      img.src = e.target?.result as string;
    };
    reader.onerror = () => resolve({ compressedFile: file, originalSize: file.size, compressedSize: file.size });

    img.onload = () => {
      const canvas = document.createElement('canvas');
      let { width, height } = img;
      const MAX_DIMENSION = 1920;

      if (width > MAX_DIMENSION || height > MAX_DIMENSION) {
        if (width > height) {
          height = Math.round((height * MAX_DIMENSION) / width);
          width = MAX_DIMENSION;
        } else {
          width = Math.round((width * MAX_DIMENSION) / height);
          height = MAX_DIMENSION;
        }
      }

      canvas.width = width;
      canvas.height = height;
      const ctx = canvas.getContext('2d');
      if (!ctx) {
        resolve({ compressedFile: file, originalSize: file.size, compressedSize: file.size });
        return;
      }

      ctx.drawImage(img, 0, 0, width, height);

      // Attempt WebP compression (0.82 quality)
      canvas.toBlob(
        (blob) => {
          if (!blob) {
            resolve({ compressedFile: file, originalSize: file.size, compressedSize: file.size });
            return;
          }

          // If compression resulted in smaller size, use it
          if (blob.size < file.size) {
            const baseName = file.name.substring(0, file.name.lastIndexOf('.')) || file.name;
            const compressedFile = new File([blob], `${baseName}.webp`, { type: 'image/webp' });
            resolve({ compressedFile, originalSize: file.size, compressedSize: compressedFile.size });
          } else {
            resolve({ compressedFile: file, originalSize: file.size, compressedSize: file.size });
          }
        },
        'image/webp',
        0.82
      );
    };

    reader.readAsDataURL(file);
  });
}

export default function PhotoUpload({ file, onFileChange, label = 'Upload Photo' }: PhotoUploadProps) {
  const [isCompressing, setIsCompressing] = useState(false);
  const [compressionInfo, setCompressionInfo] = useState<{ orig: number; comp: number } | null>(null);

  const onDrop = useCallback(async (acceptedFiles: File[]) => {
    if (acceptedFiles.length > 0) {
      const rawFile = acceptedFiles[0];
      setIsCompressing(true);
      try {
        const { compressedFile, originalSize, compressedSize } = await compressImage(rawFile);
        if (originalSize > compressedSize) {
          setCompressionInfo({ orig: originalSize, comp: compressedSize });
        } else {
          setCompressionInfo(null);
        }
        onFileChange(compressedFile);
      } catch {
        onFileChange(rawFile);
      } finally {
        setIsCompressing(false);
      }
    }
  }, [onFileChange]);

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    accept: { 'image/*': ['.png', '.jpg', '.jpeg', '.webp'] },
    maxFiles: 1,
    maxSize: 10 * 1024 * 1024,
    disabled: isCompressing,
  });

  const handleRemove = (e: React.MouseEvent) => {
    e.stopPropagation();
    setCompressionInfo(null);
    onFileChange(null);
  };

  if (isCompressing) {
    return (
      <div className="border-2 border-dashed rounded-xl p-8 text-center bg-muted/30 flex flex-col items-center justify-center gap-2">
        <Loader2 className="h-7 w-7 animate-spin text-primary" />
        <p className="text-sm font-medium">Optimizing photo for fast upload...</p>
        <p className="text-xs text-muted-foreground">Resizing and compressing image</p>
      </div>
    );
  }

  if (file) {
    return (
      <div className="relative group">
        <img
          src={URL.createObjectURL(file)}
          alt="Preview"
          className="w-full h-48 object-cover rounded-xl border"
        />
        {compressionInfo && (
          <div className="absolute bottom-2 left-2 bg-black/75 backdrop-blur-sm text-white px-2.5 py-1 rounded-lg text-xs font-medium flex items-center gap-1.5 shadow-sm">
            <CheckCircle2 className="h-3.5 w-3.5 text-emerald-400" />
            <span>
              Compressed: {formatBytes(compressionInfo.orig)} → {formatBytes(compressionInfo.comp)}
            </span>
          </div>
        )}
        <button
          onClick={handleRemove}
          className="absolute top-2 right-2 h-7 w-7 rounded-full bg-black/60 text-white flex items-center justify-center hover:bg-black/80 transition-colors shadow-sm"
          title="Remove photo"
        >
          <X className="h-4 w-4" />
        </button>
      </div>
    );
  }

  return (
    <div
      {...getRootProps()}
      className={cn(
        'border-2 border-dashed rounded-xl p-8 text-center cursor-pointer transition-all duration-200',
        isDragActive ? 'border-primary bg-primary/5' : 'border-muted-foreground/25 hover:border-primary/50 hover:bg-muted/50'
      )}
    >
      <input {...getInputProps()} />
      <Upload className="h-8 w-8 mx-auto mb-3 text-muted-foreground" />
      <p className="text-sm font-medium">{label}</p>
      <p className="text-xs text-muted-foreground mt-1">
        Drag & drop or click to browse (PNG, JPG, WebP — max 10MB)
      </p>
    </div>
  );
}
