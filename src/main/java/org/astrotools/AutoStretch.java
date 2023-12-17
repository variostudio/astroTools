package org.astrotools;

import mil.nga.tiff.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class AutoStretch {
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("Specify a TIFF file to process");
            System.exit(0);
        }
        String fileName = args[0];
        System.out.println("Processing file: " + fileName);

        String resultFile = processImage(fileName);

        System.out.println("Auto Stretching is done: " + resultFile);
    }

    private static String processImage(String fileName) throws IOException {
        TIFFImage origImage = TiffReader.readTiff(new File(fileName));
        Stretcher stretcher = new Stretcher();

        TIFFImage tiffImage = new TIFFImage();
        List<FileDirectory> fileDirectories = origImage.getFileDirectories();

        for (FileDirectory fd: fileDirectories) {
            FileDirectory directory = new FileDirectory();
            for (FileDirectoryEntry origEntry: fd.getEntries()) {
                directory.addEntry(new FileDirectoryEntry(origEntry.getFieldTag(), origEntry.getFieldType(), origEntry.getTypeCount(), origEntry.getValues()));
            }

            Rasters rasters = fd.readRasters();

            directory.setImageWidth(rasters.getWidth());
            directory.setImageHeight(rasters.getHeight());
            directory.setBitsPerSample(rasters.getBitsPerSample());
            directory.setCompression(fd.getCompression());
            directory.setPhotometricInterpretation(fd.getPhotometricInterpretation());
            directory.setSamplesPerPixel(rasters.getSamplesPerPixel());
            directory.setRowsPerStrip(rasters.calculateRowsPerStrip(fd.getPlanarConfiguration()));
            directory.setResolutionUnit(fd.getResolutionUnit());
            directory.setXResolution(fd.getXResolution());
            directory.setYResolution(fd.getYResolution());
            directory.setPlanarConfiguration(fd.getPlanarConfiguration());
            directory.setSampleFormat(fd.getSampleFormat());
            directory.setWriteRasters(stretcher.stretch(rasters));

            tiffImage.add(directory);
        }

        String autoStretchedName = autoStretchedName(fileName);
        File f = new File(autoStretchedName);
        TiffWriter.writeTiff(f, tiffImage);

        return autoStretchedName;
    }

    private static String autoStretchedName(String original) {
        int dot = original.lastIndexOf(".");
        String fileName = original.substring(0, dot);
        String ext = original.substring(dot);

        return fileName + "_stretched" + ext;
    }
}
