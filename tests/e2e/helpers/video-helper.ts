import { Page, expect } from '@playwright/test';

export class VideoHelper {
  constructor(private page: Page) {}

  /**
   * Wait for video player to appear on the page
   */
  async waitForVideoPlayer(timeout: number = 60000): Promise<void> {
    await this.page.waitForSelector('video', { timeout });
  }

  /**
   * Check if video is currently playing
   */
  async isVideoPlaying(): Promise<boolean> {
    return await this.page.evaluate(() => {
      const video = document.querySelector('video') as HTMLVideoElement;
      return video && !video.paused && video.currentTime > 0 && video.readyState > 2;
    });
  }

  /**
   * Get current video playback time
   */
  async getVideoCurrentTime(): Promise<number> {
    return await this.page.evaluate(() => {
      const video = document.querySelector('video') as HTMLVideoElement;
      return video?.currentTime || 0;
    });
  }

  /**
   * Get video duration
   */
  async getVideoDuration(): Promise<number> {
    return await this.page.evaluate(() => {
      const video = document.querySelector('video') as HTMLVideoElement;
      return video?.duration || 0;
    });
  }

  /**
   * Seek to specific time in video
   */
  async seekTo(timeInSeconds: number): Promise<void> {
    await this.page.evaluate((time) => {
      const video = document.querySelector('video') as HTMLVideoElement;
      if (video) {
        video.currentTime = time;
      }
    }, timeInSeconds);
  }

  /**
   * Play video
   */
  async play(): Promise<void> {
    await this.page.evaluate(() => {
      const video = document.querySelector('video') as HTMLVideoElement;
      if (video) {
        video.play();
      }
    });
  }

  /**
   * Pause video
   */
  async pause(): Promise<void> {
    await this.page.evaluate(() => {
      const video = document.querySelector('video') as HTMLVideoElement;
      if (video) {
        video.pause();
      }
    });
  }

  /**
   * Get video source URL
   */
  async getVideoSource(): Promise<string | null> {
    return await this.page.evaluate(() => {
      const video = document.querySelector('video') as HTMLVideoElement;
      const source = video?.querySelector('source');
      return source?.src || video?.src || null;
    });
  }

  /**
   * Select subtitle track by language code
   */
  async selectSubtitle(language: string): Promise<void> {
    await this.page.evaluate((lang) => {
      const video = document.querySelector('video') as HTMLVideoElement;
      if (video && video.textTracks) {
        for (let i = 0; i < video.textTracks.length; i++) {
          const track = video.textTracks[i];
          track.mode = track.language === lang ? 'showing' : 'hidden';
        }
      }
    }, language);
  }

  /**
   * Get list of available subtitle languages
   */
  async getAvailableSubtitles(): Promise<string[]> {
    return await this.page.evaluate(() => {
      const video = document.querySelector('video') as HTMLVideoElement;
      if (!video || !video.textTracks) return [];

      const languages: string[] = [];
      for (let i = 0; i < video.textTracks.length; i++) {
        languages.push(video.textTracks[i].language);
      }
      return languages;
    });
  }

  /**
   * Check if subtitles are currently showing
   */
  async areSubtitlesActive(): Promise<boolean> {
    return await this.page.evaluate(() => {
      const video = document.querySelector('video') as HTMLVideoElement;
      if (!video || !video.textTracks) return false;

      for (let i = 0; i < video.textTracks.length; i++) {
        if (video.textTracks[i].mode === 'showing') {
          return true;
        }
      }
      return false;
    });
  }

  /**
   * Wait for download to reach target percentage
   */
  async waitForDownloadProgress(
    targetPercent: number,
    timeout: number = 120000
  ): Promise<boolean> {
    const startTime = Date.now();

    while (Date.now() - startTime < timeout) {
      try {
        const progressElement = this.page.locator('[data-testid="download-progress"]');

        // Check if element exists
        if (await progressElement.count() > 0) {
          const progressText = await progressElement.textContent();

          if (progressText) {
            const match = progressText.match(/(\d+(?:\.\d+)?)\s*%/);
            if (match) {
              const percent = parseFloat(match[1]);
              console.log(`Current progress: ${percent}%`);

              if (percent >= targetPercent) {
                console.log(`Reached target progress: ${percent}% >= ${targetPercent}%`);
                return true;
              }
            }
          }
        }
      } catch (error) {
        console.error('Error checking progress:', error);
      }

      // Wait 2 seconds before checking again
      await this.page.waitForTimeout(2000);
    }

    throw new Error(`Download did not reach ${targetPercent}% within ${timeout}ms`);
  }

  /**
   * Get current download speed
   */
  async getDownloadSpeed(): Promise<string | null> {
    const speedElement = this.page.locator('[data-testid="download-speed"]');
    if (await speedElement.count() > 0) {
      return await speedElement.textContent();
    }
    return null;
  }

  /**
   * Get estimated time remaining
   */
  async getETA(): Promise<string | null> {
    const etaElement = this.page.locator('[data-testid="download-eta"]');
    if (await etaElement.count() > 0) {
      return await etaElement.textContent();
    }
    return null;
  }

  /**
   * Get download status
   */
  async getDownloadStatus(): Promise<string | null> {
    const statusElement = this.page.locator('[data-testid="download-status"]');
    if (await statusElement.count() > 0) {
      return await statusElement.textContent();
    }
    return null;
  }

  /**
   * Wait for video to be ready for streaming
   */
  async waitForStreamingReady(timeout: number = 180000): Promise<void> {
    console.log('Waiting for streaming to be ready...');

    // Wait for either the video player to appear or status to be completed
    await Promise.race([
      this.waitForVideoPlayer(timeout),
      this.page.waitForSelector('[data-testid="video-ready"]', { timeout })
    ]);

    console.log('Streaming is ready!');
  }

  /**
   * Verify video playback quality
   */
  async getVideoStats(): Promise<{
    width: number;
    height: number;
    readyState: number;
    networkState: number;
  }> {
    return await this.page.evaluate(() => {
      const video = document.querySelector('video') as HTMLVideoElement;
      return {
        width: video?.videoWidth || 0,
        height: video?.videoHeight || 0,
        readyState: video?.readyState || 0,
        networkState: video?.networkState || 0
      };
    });
  }

  /**
   * Check for video errors
   */
  async getVideoError(): Promise<string | null> {
    return await this.page.evaluate(() => {
      const video = document.querySelector('video') as HTMLVideoElement;
      if (video?.error) {
        return `Error code: ${video.error.code}, Message: ${video.error.message}`;
      }
      return null;
    });
  }
}
