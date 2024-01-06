package com.bot.main.bot.service;

import com.github.kiulian.downloader.YoutubeDownloader;
import com.github.kiulian.downloader.downloader.YoutubeCallback;
import com.github.kiulian.downloader.downloader.request.RequestVideoFileDownload;
import com.github.kiulian.downloader.downloader.request.RequestVideoInfo;
import com.github.kiulian.downloader.downloader.response.Response;
import com.github.kiulian.downloader.model.videos.VideoInfo;
import com.github.kiulian.downloader.model.videos.formats.Format;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VideoService {
    private final YoutubeDownloader downloader = new YoutubeDownloader();
    public void download(String videoId) {

        RequestVideoInfo requestVideoInfo = new RequestVideoInfo(videoId);
        Response<VideoInfo> response = downloader.getVideoInfo(requestVideoInfo);
        VideoInfo video = response.data();
        Format format = video.bestVideoWithAudioFormat();

        RequestVideoFileDownload requestVideoFileDownload = new RequestVideoFileDownload(format)
                .renameTo("video")
                .overwriteIfExists(true);
        downloader.downloadVideoFile(requestVideoFileDownload);
        log.info("Video successful downloaded, id: " + videoId);
    }
    public String getVideoTitle(String videoId) {
        return parseVideoInfo(videoId).details().title();
    }
    public String getVideoChannel(String videoId) {
        return parseVideoInfo(videoId).details().author();
    }

    public VideoInfo parseVideoInfo(String videoId) {
        RequestVideoInfo request = new RequestVideoInfo(videoId)
                .callback(new YoutubeCallback<>() {
                    @Override
                    public void onFinished(VideoInfo videoInfo) {
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        log.error(throwable.getMessage());
                    }
                })
                .async();
        Response<VideoInfo> response = downloader.getVideoInfo(request);
        return response.data();
    }
}
