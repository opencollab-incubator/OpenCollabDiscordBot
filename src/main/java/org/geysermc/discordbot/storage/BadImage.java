/*
 * Copyright (c) 2026 Open Collaboration. https://opencollaboration.dev, GeyserMC. https://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author OpenCollab, GeyserMC
 * @link https://github.com/OpenCollaboration/OpenCollabDiscordBot
 */

package org.geysermc.discordbot.storage;

import dev.brachtendorf.jimagehash.hash.FuzzyHash;
import dev.brachtendorf.jimagehash.hash.Hash;
import dev.brachtendorf.jimagehash.hashAlgorithms.AverageHash;
import dev.brachtendorf.jimagehash.hashAlgorithms.DifferenceHash;
import dev.brachtendorf.jimagehash.hashAlgorithms.HashingAlgorithm;
import dev.brachtendorf.jimagehash.hashAlgorithms.PerceptiveHash;
import net.dv8tion.jda.api.entities.Message;
import pw.chew.chewbotcca.util.RestClient;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

public record BadImage(String similarityHash, String b64SourceImg) {
    private static final List<HashingAlgorithm> HASHERS = List.of(
            new AverageHash(512),
            new PerceptiveHash(512),
            new DifferenceHash(512, DifferenceHash.Precision.Triple)
    );

    public BadImage(Message.Attachment attachment) {
        BufferedImage image = RestClient.getImage(attachment.getUrl());
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        String sourceB64;
        try {
            ImageIO.write(image, "PNG", os);
            sourceB64 = Base64.getEncoder().encodeToString(os.toByteArray());
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
        this(hash(image), sourceB64);
    }

    public double getSimilarity(BadImage image) {
        String[] thisHashes = this.similarityHash.split("-");
        String[] thatHashes = image.similarityHash.split("-");

        double[] hashes = new double[HASHERS.size()];

        int i = 0;
        for (HashingAlgorithm algorithm : HASHERS) {
            Hash thisHash = new Hash(new BigInteger(thisHashes[i], 16), algorithm.getKeyResolution(), algorithm.algorithmId());
            Hash thatHash = new Hash(new BigInteger(thatHashes[i], 16), algorithm.getKeyResolution(), algorithm.algorithmId());
            hashes[i] = thisHash.normalizedHammingDistance(thatHash);

            i++;
        }

        return DoubleStream.of(hashes).average().orElseThrow();
    }

    private static String hash(BufferedImage image) {
        return HASHERS.stream().map(a -> a.hash(image)).map(BadImage::stringifyHash).collect(Collectors.joining("-"));
    }

    private static String stringifyHash(Hash hash) {
        return hash.getHashValue().toString(16);
    }

}
