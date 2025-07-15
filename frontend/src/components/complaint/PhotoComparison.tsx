import { ImageIcon } from 'lucide-react';

interface Photo {
  photoUrl: string;
  photoType: 'BEFORE' | 'AFTER';
}

interface PhotoComparisonProps {
  photos: Photo[];
}

export default function PhotoComparison({ photos }: PhotoComparisonProps) {
  const beforePhoto = photos.find((p) => p.photoType === 'BEFORE');
  const afterPhoto = photos.find((p) => p.photoType === 'AFTER');

  return (
    <div className="grid grid-cols-2 gap-4">
      <div>
        <p className="text-xs font-medium text-muted-foreground mb-2 uppercase tracking-wider">Before</p>
        {beforePhoto ? (
          <div className="relative aspect-video rounded-xl overflow-hidden border bg-muted">
            <img
              src={beforePhoto.photoUrl}
              alt="Before repair"
              className="w-full h-full object-cover hover:scale-105 transition-transform duration-300"
            />
          </div>
        ) : (
          <div className="aspect-video rounded-xl border border-dashed bg-muted/50 flex flex-col items-center justify-center text-muted-foreground">
            <ImageIcon className="h-8 w-8 mb-2 opacity-40" />
            <p className="text-xs">No photo</p>
          </div>
        )}
      </div>

      <div>
        <p className="text-xs font-medium text-muted-foreground mb-2 uppercase tracking-wider">After</p>
        {afterPhoto ? (
          <div className="relative aspect-video rounded-xl overflow-hidden border bg-muted">
            <img
              src={afterPhoto.photoUrl}
              alt="After repair"
              className="w-full h-full object-cover hover:scale-105 transition-transform duration-300"
            />
          </div>
        ) : (
          <div className="aspect-video rounded-xl border border-dashed bg-muted/50 flex flex-col items-center justify-center text-muted-foreground">
            <ImageIcon className="h-8 w-8 mb-2 opacity-40" />
            <p className="text-xs">Pending repair</p>
          </div>
        )}
      </div>
    </div>
  );
}
