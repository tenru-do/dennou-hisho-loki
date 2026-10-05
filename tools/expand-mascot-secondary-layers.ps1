param(
    [string]$AssetDirectory = (Join-Path (Split-Path $PSScriptRoot -Parent) 'glass-app/assets')
)
$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
Add-Type -ReferencedAssemblies System.Drawing -TypeDefinition @'
using System;
using System.Drawing;
using System.Drawing.Imaging;
using System.Drawing.Drawing2D;
public static class MascotLayerPacker {
    static Rectangle Bounds(Bitmap sheet, int pose) {
        int left=320, top=320, right=-1, bottom=-1;
        for(int y=0;y<320;y++) for(int x=0;x<320;x++)
            if(sheet.GetPixel(pose%5*320+x,pose/5*320+y).A!=0) {
                left=Math.Min(left,x); right=Math.Max(right,x);
                top=Math.Min(top,y); bottom=Math.Max(bottom,y);
            }
        if(right<left) throw new Exception("Empty layer pose " + pose);
        return Rectangle.FromLTRB(left,top,right+1,bottom+1);
    }
    static void RectJson(System.Text.StringBuilder json, Rectangle r) {
        json.AppendFormat(System.Globalization.CultureInfo.InvariantCulture,
            "[{0},{1},{2},{3}]",r.Left,r.Top,r.Right,r.Bottom);
    }
    public static void Expand(string directory, int[] fixedFeatures) {
        using(Bitmap original=new Bitmap(System.IO.Path.Combine(directory,"v7_high_density.png")))
        using(Bitmap basis=new Bitmap(1600,960,PixelFormat.Format32bppArgb))
        using(Bitmap fringe=new Bitmap(1600,960,PixelFormat.Format32bppArgb))
        using(Bitmap sides=new Bitmap(1600,960,PixelFormat.Format32bppArgb))
        using(GraphicsPath fringeMask=new GraphicsPath())
        using(GraphicsPath leftMask=new GraphicsPath())
        using(GraphicsPath rightMask=new GraphicsPath()) {
            if(original.Width!=1600 || original.Height!=960) throw new Exception("Expected 15 x 320px poses");
            fringeMask.AddPolygon(new PointF[]{new PointF(24,0),new PointF(300,0),
                new PointF(300,92),new PointF(271,102),new PointF(250,89),
                new PointF(225,100),new PointF(197,118),new PointF(168,130),
                new PointF(137,140),new PointF(111,146),new PointF(101,164),
                new PointF(89,173),new PointF(66,155),new PointF(24,150)});
            leftMask.AddPolygon(new PointF[]{new PointF(24,60),new PointF(75,65),
                new PointF(103,160),new PointF(101,186),new PointF(93,194),
                new PointF(78,187),new PointF(55,164),new PointF(24,146)});
            rightMask.AddPolygon(new PointF[]{new PointF(269,56),new PointF(312,56),
                new PointF(312,181),new PointF(276,180),new PointF(269,138)});
            int fringePixels=0, sidePixels=0;
            for(int y=0;y<960;y++) for(int x=0;x<1600;x++) {
                Color pixel=original.GetPixel(x,y);
                int localX=x%320, localY=y%320;
                bool ink=pixel.R!=0 || pixel.G!=0 || pixel.B!=0;
                int pose=(y/320)*5+x/320;
                bool fixedPart=localX>=40 && localX<=112 && localY>=155 && localY<=214;
                for(int eye=0;eye<2;eye++) {
                    int at=pose*8+eye*4;
                    if(localX>=fixedFeatures[at]-8 && localX<=fixedFeatures[at+2]+8
                        && localY>=fixedFeatures[at+1]-24 && localY<=fixedFeatures[at+3]) fixedPart=true;
                }
                bool hair=ink && !fixedPart && (fringeMask.IsVisible(localX+.5f,localY+.5f)
                    || leftMask.IsVisible(localX+.5f,localY+.5f) || rightMask.IsVisible(localX+.5f,localY+.5f));
                bool isFringe=hair && localX>=90 && localX<246;
                bool isSide=hair && !isFringe;
                basis.SetPixel(x,y,isFringe || isSide ? Color.Black : pixel);
                if(isFringe) { fringe.SetPixel(x,y,pixel); fringePixels++; }
                else if(isSide) { sides.SetPixel(x,y,pixel); sidePixels++; }
            }
            if(fringePixels==0 || sidePixels==0) throw new Exception("Empty hair layer");
            // Pixel-level reassembly proof: layers at zero offset reconstruct the source exactly.
            for(int y=0;y<960;y++) for(int x=0;x<1600;x++) {
                Color rebuilt=basis.GetPixel(x,y), a=fringe.GetPixel(x,y), b=sides.GetPixel(x,y);
                if(a.A!=0) rebuilt=a;
                if(b.A!=0) rebuilt=b;
                if(rebuilt.ToArgb()!=original.GetPixel(x,y).ToArgb())
                    throw new Exception("Layer reassembly mismatch at "+x+","+y);
            }
            var json=new System.Text.StringBuilder("{\"size\":320,\"frames\":[");
            for(int pose=0;pose<15;pose++) {
                if(pose>0) json.Append(",");
                json.Append("{\"fringe\":"); RectJson(json,Bounds(fringe,pose));
                json.Append(",\"sidehair\":"); RectJson(json,Bounds(sides,pose));
                json.Append("}");
            }
            json.Append("]}");
            System.IO.File.WriteAllText(System.IO.Path.Combine(directory,"v7_high_density_secondary.json"),json.ToString());
            basis.Save(System.IO.Path.Combine(directory,"v7_high_density_base.png"),ImageFormat.Png);
            fringe.Save(System.IO.Path.Combine(directory,"v7_high_density_fringe.png"),ImageFormat.Png);
            sides.Save(System.IO.Path.Combine(directory,"v7_high_density_sidehair.png"),ImageFormat.Png);
            Console.WriteLine("PASS: 15 poses, 45 derived cells, 1536000 exact reassembly pixels");
        }
    }
}
'@
$manifest=Get-Content -LiteralPath (Join-Path $AssetDirectory 'v7_high_density_regions.json') -Raw -Encoding UTF8 | ConvertFrom-Json
$features=[int[]]@(foreach($frame in $manifest.frames){foreach($eye in $frame.eyes){foreach($coordinate in $eye){[int]$coordinate}}})
if($features.Length -ne 120){throw 'Expected 15 eye ROI pairs'}
[MascotLayerPacker]::Expand([IO.Path]::GetFullPath($AssetDirectory),$features)
Get-FileHash -Algorithm SHA256 (Join-Path $AssetDirectory 'v7_high_density_base.png'),(Join-Path $AssetDirectory 'v7_high_density_fringe.png'),(Join-Path $AssetDirectory 'v7_high_density_sidehair.png')
