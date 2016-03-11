%##########################################################################
% File:       PunktOperationen.m
% Purpose:    Demo for point operators and histogram functions
% Author:     Marcus Hudritsch
% Date:       15-FEB-09
% Copyright:  Marcus Hudritsch, Kirchrain 18, 2572 Sutz
%             THIS SOFTWARE IS PROVIDED FOR EDUCATIONAL PURPOSE ONLY AND
%             WITHOUT ANY WARRANTIES WHETHER EXPRESSED OR IMPLIED.
%##########################################################################
clear all; close all; clc; %clear matrices, close figures & clear cmd wnd.

X = imread('../Images/Blood2.bmp'); %liesst BMP in Matrix X 
I = im2single(X);                   %Konvertierung in Mat. I mit floats (0-1)
imshow(I);                          %Bild anzeigen
title('Original');
pause;
 
fprintf ('\n\nBerechne Statistiken ...');
Imin = min(I(:));                   %Berechne Minimum
Imax = max(I(:));                   %Berechne Maximum
Imea = mean(I(:));                  %Berechen Durchschnitt
Istd = std(I(:));                   %Berechen Standardabweichung
Ivar = var(I(:));                   %Berechne Varianz
fprintf ('\nMinimum   : %f', Imin);
fprintf ('\nMaximum   : %f', Imax);
fprintf ('\nMittelwert: %f', Imea);
fprintf ('\nStd.abw   : %f', Istd);
fprintf ('\nVarianz   : %f', Ivar);
 
fprintf ('\n\nHistogramm Bearbeitung ...');
[B,map] = gray2ind (I,256);         %Float Graustufen zu 256 Integer-Graustufen 
imhist(B);                          %Histogramm anzeigen
title('Histogramm von Original');
pause;

cumHist = cumsum(imhist(B));        %cumulative histogram.
plot(cumHist);                      %draw cumulative histogram. 
title('Cumulative Histogram');
pause;

I2 = histeq(I);                     %Histogrammausgleich
imshow(I2);
title('Nach Histogrammausgleich');
pause;
[B,map] = gray2ind(I2,256);         %Float Graustufen zu 256 Integer-Graustufen 
imhist(B);
title('Histogramm nach Ausgleich');
pause;
 
I2 = imadjust(I,[Imin Imax],[0 1]); %Kontrasterhöhung durch Spreizung
imshow(I2);
title('Nach Kontrastspreizung');
pause;
[B,map] = gray2ind(I2,256);
imhist(B);
title('Histogrammausgleich nach Kontrastspreizung');
pause;

GammaValue = 1.0/2.2;
%I2 = 255 * (I/255).^ GammaValue;   %Gammakorrektur für Byte Werte
I2 = I.^ GammaValue;                %Gammakorrektur für Floats (0-1)
imshow(I2);
title('Nach Gammakorrektur mit Gamma 1/2.2');
pause;
[B,map] = gray2ind(I2,256);
imhist(B);
title('Histogrammausgleich nach Gammakorrektur');
pause;
