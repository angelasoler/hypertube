import { request } from '@playwright/test';

export interface DownloadRequest {
  magnetUri: string;
  videoId: string;
}

export interface DownloadJobDTO {
  id: string;
  videoId: string;
  status: string;
  progress: number;
  downloadSpeed?: number;
  etaSeconds?: number;
}

export interface ReadyResponse {
  ready: boolean;
  status: string;
  progress: number;
  downloadSpeed?: number;
  etaSeconds?: number;
  peers?: number;
}

export class ApiHelper {
  private baseURL: string;
  private token?: string;

  constructor(baseURL: string) {
    this.baseURL = baseURL;
  }

  /**
   * Login and store authentication token
   */
  async login(username: string, password: string): Promise<string | undefined> {
    const context = await request.newContext({ baseURL: this.baseURL });

    try {
      const response = await context.post('/api/auth/login', {
        data: { username, password }
      });

      if (response.ok()) {
        const data = await response.json();
        this.token = data.token || data.accessToken;
        return this.token;
      } else {
        console.error('Login failed:', response.status(), await response.text());
      }
    } catch (error) {
      console.error('Login error:', error);
    } finally {
      await context.dispose();
    }

    return undefined;
  }

  /**
   * Register a new user
   */
  async register(userData: {
    username: string;
    email: string;
    password: string;
    firstName?: string;
    lastName?: string;
  }): Promise<boolean> {
    const context = await request.newContext({ baseURL: this.baseURL });

    try {
      const response = await context.post('/api/auth/register', {
        data: userData
      });

      return response.ok();
    } catch (error) {
      console.error('Registration error:', error);
      return false;
    } finally {
      await context.dispose();
    }
  }

  /**
   * Search for videos
   */
  async searchVideos(query: string, page: number = 0, filters?: any): Promise<any> {
    const context = await request.newContext({
      baseURL: this.baseURL,
      extraHTTPHeaders: this.token ? {
        'Authorization': `Bearer ${this.token}`
      } : {}
    });

    try {
      const params: any = { q: query, page };
      if (filters) {
        Object.assign(params, filters);
      }

      const response = await context.get('/api/search', { params });

      if (response.ok()) {
        return await response.json();
      }
    } catch (error) {
      console.error('Search error:', error);
    } finally {
      await context.dispose();
    }

    return null;
  }

  /**
   * Get video details
   */
  async getVideoDetails(videoId: string): Promise<any> {
    const context = await request.newContext({
      baseURL: this.baseURL,
      extraHTTPHeaders: this.token ? {
        'Authorization': `Bearer ${this.token}`
      } : {}
    });

    try {
      const response = await context.get(`/api/videos/${videoId}`);

      if (response.ok()) {
        return await response.json();
      }
    } catch (error) {
      console.error('Get video details error:', error);
    } finally {
      await context.dispose();
    }

    return null;
  }

  /**
   * Initiate video download
   */
  async initiateDownload(data: DownloadRequest): Promise<DownloadJobDTO | null> {
    const context = await request.newContext({
      baseURL: this.baseURL,
      extraHTTPHeaders: this.token ? {
        'Authorization': `Bearer ${this.token}`
      } : {}
    });

    try {
      const response = await context.post('/api/streaming/download', {
        data
      });

      if (response.ok()) {
        return await response.json();
      } else {
        console.error('Download initiation failed:', response.status(), await response.text());
      }
    } catch (error) {
      console.error('Initiate download error:', error);
    } finally {
      await context.dispose();
    }

    return null;
  }

  /**
   * Check download progress and readiness
   */
  async checkDownloadProgress(jobId: string): Promise<ReadyResponse | null> {
    const context = await request.newContext({
      baseURL: this.baseURL,
      extraHTTPHeaders: this.token ? {
        'Authorization': `Bearer ${this.token}`
      } : {}
    });

    try {
      const response = await context.get(`/api/streaming/jobs/${jobId}/ready`);

      if (response.ok()) {
        return await response.json();
      }
    } catch (error) {
      console.error('Check progress error:', error);
    } finally {
      await context.dispose();
    }

    return null;
  }

  /**
   * Get available subtitles for a video
   */
  async getSubtitles(videoId: string): Promise<any[]> {
    const context = await request.newContext({
      baseURL: this.baseURL,
      extraHTTPHeaders: this.token ? {
        'Authorization': `Bearer ${this.token}`
      } : {}
    });

    try {
      const response = await context.get(`/api/streaming/subtitles/${videoId}`);

      if (response.ok()) {
        return await response.json();
      }
    } catch (error) {
      console.error('Get subtitles error:', error);
    } finally {
      await context.dispose();
    }

    return [];
  }

  /**
   * Get authentication token
   */
  getToken(): string | undefined {
    return this.token;
  }
}
