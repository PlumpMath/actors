/*
 * Copyright (c) 2017, Kasra Faghihi, All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package com.offbynull.actors.core.cache;

import com.offbynull.actors.core.context.Serializer;
import com.offbynull.actors.core.context.SourceContext;
import com.offbynull.actors.core.shuttle.Address;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Saves and restores actors via the filesystem.
 *
 * @author Kasra Faghihi
 */
public final class FileSystemCacher implements Cacher {

    private static final Logger LOG = LoggerFactory.getLogger(FileSystemCacher.class);

    private final Path savedDirectory;
    private final Path restoredDirectory;
    private final Serializer serializer;

    /**
     * Create a {@link FileSystemCacher} object.
     *
     * @param serializer serializer to use for saving/restoring actors
     * @param directory storage directory for serialized actors
     * @return new instance of {@link FileSystemCacher}
     */
    public static FileSystemCacher create(Serializer serializer, Path directory) {
        Validate.notNull(serializer);
        Validate.notNull(directory);

        Path savedDirectory = directory.resolve("saved");
        Path restoredDirectory = directory.resolve("restored");
        try {
            Files.createDirectories(savedDirectory);
            Files.createDirectories(restoredDirectory);
        } catch (IOException ioe) {
            throw new IllegalArgumentException(ioe);
        }

        return new FileSystemCacher(serializer, savedDirectory, restoredDirectory);
    }

    private FileSystemCacher(Serializer serializer, Path savedDirectory, Path restoredDirectory) {
        Validate.notNull(serializer);
        Validate.notNull(savedDirectory);
        Validate.notNull(restoredDirectory);
        this.serializer = serializer;
        this.savedDirectory = savedDirectory;
        this.restoredDirectory = restoredDirectory;
    }

    @Override
    public boolean save(SourceContext ctx) {
        Validate.notNull(ctx);
        Validate.isTrue(ctx.isRoot());

        Address address = ctx.self();
        byte[] data = serializer.serialize(ctx);

        String filename;
        try {
            filename = URLEncoder.encode(address.toString(), "UTF-8");
        } catch (UnsupportedEncodingException use) {
            LOG.error("Unable to encode filename {}", address, use);
            return false;
        }

        Path filepath = savedDirectory.resolve(filename);
        try {
            Files.write(filepath, data);
        } catch (IOException ioe) {
            LOG.error("Unable to write file {}", filepath, ioe);
            return false;
        }

        return true;
    }

    @Override
    public SourceContext restore(Address address) {
        Validate.notNull(address);
        
        String filename;
        try {
            filename = URLEncoder.encode(address.toString(), "UTF-8");
        } catch (UnsupportedEncodingException use) {
            LOG.error("Unable to encode filename {}", address, use);
            return null;
        }

        Path savedFilepath = savedDirectory.resolve(filename);
        Path restoredFilepath = restoredDirectory.resolve(filename);
        byte[] data;
        try {
            data = Files.readAllBytes(savedFilepath);
        } catch (IOException ioe) {
            LOG.error("Unable to read file {}", savedFilepath, ioe);
            return null;
        }

        SourceContext ctx;
        try {
            ctx = serializer.unserialize(data);
        } catch (IllegalArgumentException iae) {
            LOG.error("Unable to unserialize file {}", savedFilepath, iae);
            return null;
        }

        if (!ctx.isRoot()) {
            LOG.error("Context is not root {}", savedFilepath);
            return null;
        }

        try {
            Files.move(savedFilepath, restoredFilepath, REPLACE_EXISTING, ATOMIC_MOVE);
        } catch (IOException ioe) {
            LOG.error("Unable to move file {} to {}", savedFilepath, restoredFilepath, ioe);
            return null;
        }

        return ctx;
    }

    @Override
    public void delete(Address address) {
        Validate.notNull(address);
        
        String filename;
        try {
            filename = URLEncoder.encode(address.toString(), "UTF-8");
        } catch (UnsupportedEncodingException use) {
            LOG.error("Unable to encode filename {}", address, use);
            return;
        }

        Path savePath = savedDirectory.resolve(filename);
        try {
            Files.delete(savePath);
        } catch (IOException ioe) {
            LOG.error("Unable to delete file {} ({})", savePath, ioe.toString());
            return;
        }

        Path restorePath = restoredDirectory.resolve(filename);
        try {
            Files.delete(restorePath);
        } catch (IOException ioe) {
            LOG.error("Unable to delete file {} ({})", restorePath, ioe.toString());
            return;
        }
    }
    
    @Override
    public void close() {
        // do nothing
    }
}
