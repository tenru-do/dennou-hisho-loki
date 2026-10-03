param(
    [string]$InputAtlas = (Join-Path $PSScriptRoot 'turn-reference-raw.png'),
    [string]$OutputDirectory = (Join-Path (Split-Path $PSScriptRoot -Parent) 'glass-app/assets')
)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
Add-Type -ReferencedAssemblies System.Drawing -TypeDefinition @'
using System;
using System.Drawing;
using System.Drawing.Imaging;
using System.Drawing.Drawing2D;
public static class DenseTurnPacker {
    static Bitmap Normalize(Bitmap atlas, int index) {
        int x0 = (index % 4) * atlas.Width / 4;
        int x1 = (index % 4 + 1) * atlas.Width / 4;
        int y0 = (index / 4) * atlas.Height / 8;
        int y1 = (index / 4 + 1) * atlas.Height / 8;
        int minX=x1, minY=y1, maxX=x0, maxY=y0;
        for(int y=y0;y<y1;y++) {
            int rowInk=0;
            for(int x=x0;x<x1;x++) { Color p=atlas.GetPixel(x,y); if(p.G>65 && p.B>45) rowInk++; }
            if(rowInk<6) continue;
            for(int x=x0;x<x1;x++) {
            Color p=atlas.GetPixel(x,y);
            if(p.G>65 && p.B>45) {
                minX=Math.Min(minX,x); maxX=Math.Max(maxX,x);
                minY=Math.Min(minY,y); maxY=Math.Max(maxY,y);
            }
        }
        }
        if(maxX<=minX || maxY<=minY) throw new Exception("Empty pose " + index);
        if(minX<=x0 || maxX>=x1-1)
            throw new Exception("Clipped pose " + index + " bbox="+minX+","+minY+","+maxX+","+maxY+" cell="+x0+","+y0+","+x1+","+y1);
        // Fix vertical scale and head center; do not flatten the yaw's width changes.
        float scale=280f/(maxY-minY+1);
        float width=(maxX-minX+1)*scale;
        if(width>304f) throw new Exception("Pose too wide after registration " + index);
        Bitmap cell=new Bitmap(320,320,PixelFormat.Format32bppArgb);
        using(Graphics g=Graphics.FromImage(cell)) {
            g.Clear(Color.Black);
            g.InterpolationMode=InterpolationMode.HighQualityBicubic;
            g.PixelOffsetMode=PixelOffsetMode.HighQuality;
            g.DrawImage(atlas,new RectangleF((320f-width)*.5f,24f,width,280f),
                new RectangleF(minX,minY,maxX-minX+1,maxY-minY+1),GraphicsUnit.Pixel);
        }
        return cell;
    }
    static Rectangle Feature(Bitmap cell, int left, int right, bool mouth) {
        int top=mouth?218:158, bottom=mouth?267:191;
        int bestY=top, best=-1;
        for(int y=top;y<bottom;y++) {
            int ink=0;
            for(int x=left;x<right;x++) { Color p=cell.GetPixel(x,y); if(p.G>80 && p.B>55) ink++; }
            if(ink>best) { best=ink; bestY=y; }
        }
        if(best<3) throw new Exception("Cannot locate facial feature");
        int sum=0, count=0;
        for(int y=Math.Max(top,bestY-2);y<=Math.Min(bottom-1,bestY+2);y++)
            for(int x=left;x<right;x++) { Color p=cell.GetPixel(x,y); if(p.G>80 && p.B>55) { sum+=x; count++; } }
        int width=mouth?96:64, height=mouth?40:24;
        return new Rectangle(Math.Max(0,Math.Min(320-width,sum/count-width/2)),
            Math.Max(0,Math.Min(320-height,bestY-height/2)),width,height);
    }
    static void RectJson(System.Text.StringBuilder json, Rectangle r) {
        json.AppendFormat(System.Globalization.CultureInfo.InvariantCulture,
            "[{0},{1},{2},{3}]",r.Left,r.Top,r.Right,r.Bottom);
    }
    public static void Pack(string source, string destination) {
        using(Bitmap atlas=new Bitmap(source)) {
            if(Math.Abs(atlas.Width*2-atlas.Height)>8)
                throw new Exception("Input must be a 4-column by 8-row square-cell atlas");
            using(Bitmap faces=new Bitmap(1600,960,PixelFormat.Format32bppArgb))
            using(Bitmap eyes=new Bitmap(320,144,PixelFormat.Format32bppArgb))
            using(Graphics faceGraphics=Graphics.FromImage(faces))
            using(Graphics eyeGraphics=Graphics.FromImage(eyes)) {
                faceGraphics.Clear(Color.Black); eyeGraphics.Clear(Color.Black);
                var json=new System.Text.StringBuilder("{\"size\":320,\"frames\":[");
                for(int i=0;i<15;i++) {
                    using(Bitmap open=Normalize(atlas,i))
                    using(Bitmap closed=Normalize(atlas,16+i)) {
                        int x=i%5, y=i/5;
                        faceGraphics.DrawImageUnscaled(open,x*320,y*320);
                        if(i>0) json.Append(",");
                        json.Append("{\"eyes\":[");
                        for(int eye=0;eye<2;eye++) {
                            int left=eye==0?100:196, right=eye==0?192:279;
                            Rectangle openRegion=Feature(open,left,right,false);
                            Rectangle closedRegion=Feature(closed,left,right,false);
                            if(eye>0) json.Append(",");
                            RectJson(json,openRegion);
                            int tile=i*2+eye;
                            eyeGraphics.DrawImage(closed,new Rectangle(tile%5*64,tile/5*24,64,24),
                                closedRegion,GraphicsUnit.Pixel);
                        }
                        json.Append("],\"mouth\":");
                        RectJson(json,Feature(open,112,237,true));
                        json.Append("}");
                    }
                }
                json.Append("]}");
                System.IO.File.WriteAllText(System.IO.Path.Combine(destination,"v7_high_density_regions.json"),json.ToString());
                faces.Save(System.IO.Path.Combine(destination,"v7_high_density.png"),ImageFormat.Png);
                eyes.Save(System.IO.Path.Combine(destination,"v7_high_density_eyes_closed.png"),ImageFormat.Png);
            }
        }
    }
}
'@
New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null
[DenseTurnPacker]::Pack((Resolve-Path -LiteralPath $InputAtlas).Path, [IO.Path]::GetFullPath($OutputDirectory))
Get-FileHash -Algorithm SHA256 (Join-Path $OutputDirectory 'v7_high_density.png'),(Join-Path $OutputDirectory 'v7_high_density_eyes_closed.png')
