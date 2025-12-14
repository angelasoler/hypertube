-- V3: Seed sample video data for testing
-- This migration adds 8 popular movies with metadata, genres, and torrent sources

-- Sample Movie 1: Inception (2010)
INSERT INTO videos (id, imdb_id, title, year, runtime_minutes, synopsis, imdb_rating, poster_url, backdrop_url, language)
VALUES (
    '11111111-1111-1111-1111-111111111111',
    'tt1375666',
    'Inception',
    2010,
    148,
    'A thief who steals corporate secrets through the use of dream-sharing technology is given the inverse task of planting an idea into the mind of a C.E.O.',
    8.8,
    'https://image.tmdb.org/t/p/w500/9gk7adHYeDvHkCSEqAvQNLV5Uge.jpg',
    'https://image.tmdb.org/t/p/original/s3TBrRGB1iav7gFOCNx3H31MoES.jpg',
    'en'
);

INSERT INTO video_genres (video_id, genre_id)
SELECT '11111111-1111-1111-1111-111111111111', id FROM genres WHERE name IN ('Action', 'Science Fiction', 'Thriller');

INSERT INTO video_sources (id, video_id, quality, source_type, torrent_hash, magnet_url, seeds, peers, size_bytes)
VALUES (
    gen_random_uuid(),
    '11111111-1111-1111-1111-111111111111',
    '1080p',
    'TORRENT',
    '1111111111111111111111111111111111111111',  -- 40-char hash
    'magnet:?xt=urn:btih:1111111111111111111111111111111111111111&dn=Inception.2010.1080p.BluRay&tr=udp://tracker.example.com:80',
    150,
    45,
    2147483648
);

-- Sample Movie 2: The Dark Knight (2008)
INSERT INTO videos (id, imdb_id, title, year, runtime_minutes, synopsis, imdb_rating, poster_url, backdrop_url, language)
VALUES (
    '22222222-2222-2222-2222-222222222222',
    'tt0468569',
    'The Dark Knight',
    2008,
    152,
    'When the menace known as the Joker wreaks havoc and chaos on the people of Gotham, Batman must accept one of the greatest psychological and physical tests of his ability to fight injustice.',
    9.0,
    'https://image.tmdb.org/t/p/w500/qJ2tW6WMUDux911r6m7haRef0WH.jpg',
    'https://image.tmdb.org/t/p/original/hkBaDkMWbLaf8B1lsWsKX7Ew3Xq.jpg',
    'en'
);

INSERT INTO video_genres (video_id, genre_id)
SELECT '22222222-2222-2222-2222-222222222222', id FROM genres WHERE name IN ('Action', 'Crime', 'Drama');

INSERT INTO video_sources (id, video_id, quality, source_type, torrent_hash, magnet_url, seeds, peers, size_bytes)
VALUES (
    gen_random_uuid(),
    '22222222-2222-2222-2222-222222222222',
    '1080p',
    'TORRENT',
    '2222222222222222222222222222222222222222',
    'magnet:?xt=urn:btih:2222222222222222222222222222222222222222&dn=The.Dark.Knight.2008.1080p.BluRay&tr=udp://tracker.example.com:80',
    200,
    60,
    2415919104
);

-- Sample Movie 3: Interstellar (2014)
INSERT INTO videos (id, imdb_id, title, year, runtime_minutes, synopsis, imdb_rating, poster_url, backdrop_url, language)
VALUES (
    '33333333-3333-3333-3333-333333333333',
    'tt0816692',
    'Interstellar',
    2014,
    169,
    'A team of explorers travel through a wormhole in space in an attempt to ensure humanity''s survival.',
    8.6,
    'https://image.tmdb.org/t/p/w500/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg',
    'https://image.tmdb.org/t/p/original/pbrkL804c8yAv3zBZR4QPEafpAR.jpg',
    'en'
);

INSERT INTO video_genres (video_id, genre_id)
SELECT '33333333-3333-3333-3333-333333333333', id FROM genres WHERE name IN ('Adventure', 'Drama', 'Science Fiction');

INSERT INTO video_sources (id, video_id, quality, source_type, torrent_hash, magnet_url, seeds, peers, size_bytes)
VALUES (
    gen_random_uuid(),
    '33333333-3333-3333-3333-333333333333',
    '1080p',
    'TORRENT',
    '3333333333333333333333333333333333333333',
    'magnet:?xt=urn:btih:3333333333333333333333333333333333333333&dn=Interstellar.2014.1080p.BluRay&tr=udp://tracker.example.com:80',
    180,
    50,
    3221225472
);

-- Sample Movie 4: The Matrix (1999)
INSERT INTO videos (id, imdb_id, title, year, runtime_minutes, synopsis, imdb_rating, poster_url, backdrop_url, language)
VALUES (
    '44444444-4444-4444-4444-444444444444',
    'tt0133093',
    'The Matrix',
    1999,
    136,
    'A computer hacker learns from mysterious rebels about the true nature of his reality and his role in the war against its controllers.',
    8.7,
    'https://image.tmdb.org/t/p/w500/f89U3ADr1oiB1s9GkdPOEpXUk5H.jpg',
    'https://image.tmdb.org/t/p/original/icmmSD4vTTDKOq2vvdulafOGw93.jpg',
    'en'
);

INSERT INTO video_genres (video_id, genre_id)
SELECT '44444444-4444-4444-4444-444444444444', id FROM genres WHERE name IN ('Action', 'Science Fiction');

INSERT INTO video_sources (id, video_id, quality, source_type, torrent_hash, magnet_url, seeds, peers, size_bytes)
VALUES (
    gen_random_uuid(),
    '44444444-4444-4444-4444-444444444444',
    '720p',
    'TORRENT',
    '4444444444444444444444444444444444444444',
    'magnet:?xt=urn:btih:4444444444444444444444444444444444444444&dn=The.Matrix.1999.720p.BluRay&tr=udp://tracker.example.com:80',
    120,
    35,
    1610612736
);

-- Sample Movie 5: Parasite (2019)
INSERT INTO videos (id, imdb_id, title, year, runtime_minutes, synopsis, imdb_rating, poster_url, backdrop_url, language)
VALUES (
    '55555555-5555-5555-5555-555555555555',
    'tt6751668',
    'Parasite',
    2019,
    132,
    'All unemployed, Ki-taek''s family takes peculiar interest in the wealthy and glamorous Parks for their livelihood until they get entangled in an unexpected incident.',
    8.6,
    'https://image.tmdb.org/t/p/w500/7IiTTgloJzvGI1TAYymCfbfl3vT.jpg',
    'https://image.tmdb.org/t/p/original/TU9NIjwzjoKPwQHoHshkFcQUCG.jpg',
    'en'
);

INSERT INTO video_genres (video_id, genre_id)
SELECT '55555555-5555-5555-5555-555555555555', id FROM genres WHERE name IN ('Comedy', 'Thriller', 'Drama');

INSERT INTO video_sources (id, video_id, quality, source_type, torrent_hash, magnet_url, seeds, peers, size_bytes)
VALUES (
    gen_random_uuid(),
    '55555555-5555-5555-5555-555555555555',
    '1080p',
    'TORRENT',
    '5555555555555555555555555555555555555555',
    'magnet:?xt=urn:btih:5555555555555555555555555555555555555555&dn=Parasite.2019.1080p.BluRay&tr=udp://tracker.example.com:80',
    95,
    28,
    1879048192
);

-- Sample Movie 6: The Shawshank Redemption (1994)
INSERT INTO videos (id, imdb_id, title, year, runtime_minutes, synopsis, imdb_rating, poster_url, backdrop_url, language)
VALUES (
    '66666666-6666-6666-6666-666666666666',
    'tt0111161',
    'The Shawshank Redemption',
    1994,
    142,
    'Two imprisoned men bond over a number of years, finding solace and eventual redemption through acts of common decency.',
    9.3,
    'https://image.tmdb.org/t/p/w500/q6y0Go1tsGEsmtFryDOJo3dEmqu.jpg',
    'https://image.tmdb.org/t/p/original/kXfqcdQKsToO0OUXHcrrNCHDBzO.jpg',
    'en'
);

INSERT INTO video_genres (video_id, genre_id)
SELECT '66666666-6666-6666-6666-666666666666', id FROM genres WHERE name IN ('Drama', 'Crime');

INSERT INTO video_sources (id, video_id, quality, source_type, torrent_hash, magnet_url, seeds, peers, size_bytes)
VALUES (
    gen_random_uuid(),
    '66666666-6666-6666-6666-666666666666',
    '1080p',
    'TORRENT',
    '6666666666666666666666666666666666666666',
    'magnet:?xt=urn:btih:6666666666666666666666666666666666666666&dn=The.Shawshank.Redemption.1994.1080p&tr=udp://tracker.example.com:80',
    250,
    75,
    2013265920
);

-- Sample Movie 7: Pulp Fiction (1994)
INSERT INTO videos (id, imdb_id, title, year, runtime_minutes, synopsis, imdb_rating, poster_url, backdrop_url, language)
VALUES (
    '77777777-7777-7777-7777-777777777777',
    'tt0110912',
    'Pulp Fiction',
    1994,
    154,
    'The lives of two mob hitmen, a boxer, a gangster and his wife, and a pair of diner bandits intertwine in four tales of violence and redemption.',
    8.9,
    'https://image.tmdb.org/t/p/w500/d5iIlFn5s0ImszYzBPb8JPIfbXD.jpg',
    'https://image.tmdb.org/t/p/original/suaEOtk1N1sgg2MTM7oZd2cfVp3.jpg',
    'en'
);

INSERT INTO video_genres (video_id, genre_id)
SELECT '77777777-7777-7777-7777-777777777777', id FROM genres WHERE name IN ('Thriller', 'Crime');

INSERT INTO video_sources (id, video_id, quality, source_type, torrent_hash, magnet_url, seeds, peers, size_bytes)
VALUES (
    gen_random_uuid(),
    '77777777-7777-7777-7777-777777777777',
    '720p',
    'TORRENT',
    '7777777777777777777777777777777777777777',
    'magnet:?xt=urn:btih:7777777777777777777777777777777777777777&dn=Pulp.Fiction.1994.720p.BluRay&tr=udp://tracker.example.com:80',
    140,
    42,
    1717986918
);

-- Sample Movie 8: The Godfather (1972)
INSERT INTO videos (id, imdb_id, title, year, runtime_minutes, synopsis, imdb_rating, poster_url, backdrop_url, language)
VALUES (
    '88888888-8888-8888-8888-888888888888',
    'tt0068646',
    'The Godfather',
    1972,
    175,
    'The aging patriarch of an organized crime dynasty transfers control of his clandestine empire to his reluctant son.',
    9.2,
    'https://image.tmdb.org/t/p/w500/3bhkrj58Vtu7enYsRolD1fZdja1.jpg',
    'https://image.tmdb.org/t/p/original/tmU7GeKVybMWFButWEGl2M4GeiP.jpg',
    'en'
);

INSERT INTO video_genres (video_id, genre_id)
SELECT '88888888-8888-8888-8888-888888888888', id FROM genres WHERE name IN ('Drama', 'Crime');

INSERT INTO video_sources (id, video_id, quality, source_type, torrent_hash, magnet_url, seeds, peers, size_bytes)
VALUES (
    gen_random_uuid(),
    '88888888-8888-8888-8888-888888888888',
    '1080p',
    'TORRENT',
    '8888888888888888888888888888888888888888',
    'magnet:?xt=urn:btih:8888888888888888888888888888888888888888&dn=The.Godfather.1972.1080p.BluRay&tr=udp://tracker.example.com:80',
    220,
    65,
    2684354560
);
