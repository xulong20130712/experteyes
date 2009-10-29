/*
 * Copyright (c) 2009 by Thomas Busey and Ruj Akavipat
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Experteyes nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY Thomas Busey and Ruj Akavipat ''AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL Thomas Busey and Ruj Akavipat BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package eyetrackercalibrator.framemanaging;

/**
 * This class helps synchronize eye frames and scene frames.
 * @author ruj
 */
public class FrameSynchronizor {

    int currentBlock = 0;
    int currentFrame = 0;

    class SyncBlock {

        /** start frame number */
        public int start = 1;
        public int end = Integer.MAX_VALUE;
        /* Eye frame number */
        public int startEyeFrame = 1;
        public int endEyeFrame = Integer.MAX_VALUE;
        /* Strrting scene frame number */
        public int startSceneFrame = 1;
        public int endSceneFrame = Integer.MAX_VALUE;
    };
    SyncBlock[] syncBlocks;

    public FrameSynchronizor() {
        clearSyncPoints();
    }

    private void clearSyncPoints() {
        this.syncBlocks = new SyncBlock[1];
        this.syncBlocks[0] = new SyncBlock();
        this.currentBlock = 0;
    }

    /**
     * Setting synchronization points.  Empty or null input will make the
     * system assume that the stream is already sync.
     *
     * The points must not overlap.  If they do, the the output behavior is
     * unknown.
     */
    public void setSynchronizationPoints(SynchronizationPoint[] points) {
        if (points == null || points.length == 0) {
            // Just clear the sync point
            clearSyncPoints();
        } else {
            int total = points.length;
            // Populate accordingly
            this.syncBlocks = new SyncBlock[total];

            // Populate the first one
            this.syncBlocks[0] = new SyncBlock();

            // Compute offset just for the first block
            if (points[0].eyeFrame > points[0].sceneFrame) {
                this.syncBlocks[0].startEyeFrame = points[0].eyeFrame - points[0].sceneFrame + 1;
            } else {
                this.syncBlocks[0].startSceneFrame = points[0].sceneFrame - points[0].eyeFrame + 1;
            }

            // Compute offset for all following blocks
            for (int i = 1; i < points.length; i++) {
                // Compute the end points for the previous block
                this.syncBlocks[i - 1].endEyeFrame = points[i].eyeFrame - 1;
                this.syncBlocks[i - 1].endSceneFrame = points[i].sceneFrame - 1;

                int length = this.syncBlocks[i - 1].endEyeFrame - this.syncBlocks[i - 1].startEyeFrame;
                length = Math.max(length,
                        this.syncBlocks[i - 1].endSceneFrame - this.syncBlocks[i - 1].startSceneFrame);
                this.syncBlocks[i - 1].end = this.syncBlocks[i - 1].start + length;

                // Set the current block values
                this.syncBlocks[i].startEyeFrame = points[i].eyeFrame;
                this.syncBlocks[i].startSceneFrame = points[i].sceneFrame;
                this.syncBlocks[i].start = this.syncBlocks[i - 1].end + 1;
            }
        }
    }

    public void setCurrentFrame(int currentFrame) {
        this.currentFrame = currentFrame;

        SyncBlock block = this.syncBlocks[this.currentBlock];
        /* Check if the current frame is in the current block */
        if (currentFrame > block.start) {
            // Search to the right
            for (int i = this.currentBlock + 1; i < this.syncBlocks.length; i++) {
                SyncBlock syncBlock = syncBlocks[i];
                if (currentFrame >= syncBlock.start && currentFrame <= syncBlock.end) {
                    // Stop searching
                    this.currentBlock = i;
                    return;
                }
            }
            // Stop searching
            this.currentBlock = this.syncBlocks.length - 1;
            return;
        } else if (currentFrame < block.start) {
            // Search to the left
            for (int i = this.currentBlock - 1; i > 0; i--) {
                SyncBlock syncBlock = syncBlocks[i];
                if (currentFrame >= syncBlock.start && currentFrame <= syncBlock.end) {
                    // Stop searching
                    this.currentBlock = i;
                    return;
                }
            }
            // Stop searching
            this.currentBlock = 0;
            return;
        }
        // No need to change block
    }

    /** Get eye frame number according to previously set current frame */
    public int getEyeFrame() {
        SyncBlock block = this.syncBlocks[this.currentBlock];

        // Look up from the current block
        int frame = this.currentFrame - block.start + block.startEyeFrame;

        if (frame <= block.endEyeFrame) {
            return frame;
        } else {
            return -1;
        }
    }

    /**
     * Get eye frame number according to the current frame and also set the current
     * frame as a new current frame
     */
    public int getEyeFrame(int currentFrame) {
        setCurrentFrame(currentFrame);
        return getEyeFrame();
    }

    /** Get scene frame number according to previously set current frame */
    public int getSceneFrame() {
        SyncBlock block = this.syncBlocks[this.currentBlock];

        // Look up from the current block
        int frame = this.currentFrame - block.start + block.startSceneFrame;

        if (frame <= block.endSceneFrame) {
            return frame;
        } else {
            return -1;
        }
    }

    /**
     * Get scene frame number according to the current frame and also set the current
     * frame as a new current frame
     */
    public int getSceneFrame(int currentFrame) {
        setCurrentFrame(currentFrame);
        return getSceneFrame();
    }
}
