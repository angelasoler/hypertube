// Test video data for MVP
// These are public domain/creative commons videos for testing

export interface TestVideo {
  id: string
  title: string
  year: number
  description: string
  longDescription?: string
  thumbnail: string
  magnetLink: string
  torrentUrl?: string
  rating?: number
  duration?: string
  genre?: string
  cast?: string[]
  director?: string
  jobId?: string  // Pre-existing download job ID for testing
}

export const testVideos: TestVideo[] = [
  {
    id: 'test-video',
    title: 'Test Video - Streaming Demo',
    year: 2025,
    description: 'A short test video to demonstrate HyperTube\'s streaming capabilities with HTTP Range support.',
    longDescription: 'This is a generated test video created with FFmpeg to demonstrate the complete video streaming workflow in HyperTube. It showcases progressive streaming, HTTP Range request support, and the video player integration.',
    thumbnail: 'https://via.placeholder.com/400x300/1e3a8a/60a5fa?text=Test+Video',
    magnetLink: 'magnet:?xt=urn:btih:test',
    rating: 10.0,
    duration: '00:10',
    genre: 'Test, Demo',
    director: 'HyperTube Team',
    cast: ['FFmpeg', 'LibX264'],
    jobId: 'a14015e0-b23c-4ce9-b6af-05026cbd049a', // Pre-existing completed job
  },
  {
    id: 'sintel',
    title: 'Sintel',
    year: 2010,
    description: 'A short animated film about a lonely girl and her dragon companion.',
    longDescription: 'Sintel is an independently produced short film, initiated by the Blender Foundation as a means to further improve and validate the free/open source 3D creation suite Blender. The film follows a girl named Sintel who is searching for a baby dragon she calls Scales.',
    thumbnail: 'https://upload.wikimedia.org/wikipedia/commons/thumb/8/88/Sintel_poster.jpg/220px-Sintel_poster.jpg',
    magnetLink: 'magnet:?xt=urn:btih:08ada5a7a6183aae1e09d831df6748d566095a10&dn=Sintel&tr=udp%3A%2F%2Fexplodie.org%3A6969&tr=udp%3A%2F%2Ftracker.coppersurfer.tk%3A6969&tr=udp%3A%2F%2Ftracker.empire-js.us%3A1337&tr=udp%3A%2F%2Ftracker.leechers-paradise.org%3A6969&tr=udp%3A%2F%2Ftracker.opentrackr.org%3A1337&tr=wss%3A%2F%2Ftracker.btorrent.xyz&tr=wss%3A%2F%2Ftracker.fastcast.nz&tr=wss%3A%2F%2Ftracker.openwebtorrent.com',
    rating: 7.6,
    duration: '14:48',
    genre: 'Animation, Fantasy',
    director: 'Colin Levy',
    cast: ['Halina Reijn', 'Thom Hoffman'],
  },
  {
    id: 'big-buck-bunny',
    title: 'Big Buck Bunny',
    year: 2008,
    description: 'A comedic short film featuring a giant rabbit with a heart bigger than his ears.',
    longDescription: 'Big Buck Bunny is a 2008 computer-animated comedy short film featuring animals of the forest, made by the Blender Institute. The film follows a day in the life of a giant rabbit as he meets a friendly flying squirrel, encounters three bullies, and plots his revenge.',
    thumbnail: 'https://upload.wikimedia.org/wikipedia/commons/thumb/c/c5/Big_buck_bunny_poster_big.jpg/220px-Big_buck_bunny_poster_big.jpg',
    magnetLink: 'magnet:?xt=urn:btih:dd8255ecdc7ca55fb0bbf81323d87062db1f6d1c&dn=Big+Buck+Bunny&tr=udp%3A%2F%2Fexplodie.org%3A6969&tr=udp%3A%2F%2Ftracker.coppersurfer.tk%3A6969&tr=udp%3A%2F%2Ftracker.empire-js.us%3A1337&tr=udp%3A%2F%2Ftracker.leechers-paradise.org%3A6969&tr=udp%3A%2F%2Ftracker.opentrackr.org%3A1337&tr=wss%3A%2F%2Ftracker.btorrent.xyz&tr=wss%3A%2F%2Ftracker.fastcast.nz&tr=wss%3A%2F%2Ftracker.openwebtorrent.com',
    rating: 7.1,
    duration: '09:56',
    genre: 'Animation, Comedy',
    director: 'Sacha Goedegebure',
    cast: ['Watse Sybesma', 'Frank Smit'],
  },
  {
    id: 'tears-of-steel',
    title: 'Tears of Steel',
    year: 2012,
    description: 'A sci-fi short film set in post-apocalyptic Amsterdam.',
    longDescription: 'Tears of Steel was realized with crowd-funding by users of the open source 3D creation tool Blender. The film follows a group of warriors and scientists who gather at the "Oude Kerk" in Amsterdam to stage a crucial event from the past in a desperate attempt to rescue the world from destructive robots.',
    thumbnail: 'https://upload.wikimedia.org/wikipedia/commons/thumb/6/6a/Mango_Teaser_Poster.jpg/220px-Mango_Teaser_Poster.jpg',
    magnetLink: 'magnet:?xt=urn:btih:209c8226b299b308beaf2b9cd3fb49212dbd13ec&dn=Tears+of+Steel&tr=udp%3A%2F%2Fexplodie.org%3A6969&tr=udp%3A%2F%2Ftracker.coppersurfer.tk%3A6969&tr=udp%3A%2F%2Ftracker.empire-js.us%3A1337&tr=udp%3A%2F%2Ftracker.leechers-paradise.org%3A6969&tr=udp%3A%2F%2Ftracker.opentrackr.org%3A1337',
    rating: 7.5,
    duration: '12:14',
    genre: 'Sci-Fi, Action',
    director: 'Ian Hubert',
    cast: ['Derek de Lint', 'Sergio Hasselbaink', 'Vanja Rukavina'],
  },
]
