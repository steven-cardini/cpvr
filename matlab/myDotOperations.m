clear all; close all; clc; %clear matrices, close figures & clear cmd wnd.

% Originalbild einlesen und anzeigen
X = imread('../img/20130707-cst-2185_bw.jpg'); %liest BMP in Matrix X 
I = im2single(X);                   %Konvertierung in Mat. I mit floats (0-1)
imshow(I);                          %Bild anzeigen
title('Original');
pause;

% Lineare Helligkeitskorrektur
subplot(2,3,1), imshow(I);
title('Original');
I_bright = I + 0.10;
subplot(2,3,2), imshow(I_bright);
title('10% heller');
I_dark = I - 0.10;
subplot(2,3,3), imshow(I_dark);
title('10% dunkler');

[B,map] = gray2ind (I,256);         %Float Graustufen zu 256 Integer-Graustufen 
subplot(2,3,4), imhist(B);                          %Histogramm anzeigen
title('Histogramm original');
[B_bright,map] = gray2ind (I_bright,256);         %Float Graustufen zu 256 Integer-Graustufen 
subplot(2,3,5), imhist(B_bright);                                 %Histogramm anzeigen
title('Histogramm heller');
[B_dark,map] = gray2ind (I_dark,256);         %Float Graustufen zu 256 Integer-Graustufen 
subplot(2,3,6), imhist(B_dark);                                 %Histogramm anzeigen
title('Histogramm dunkler');
saveas(gcf,'linear_brightness_adaptation.png');
pause;