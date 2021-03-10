import { NativeModules } from 'react-native';

type BackgroundUploadType = {
  startBackgroundUploadVideo(
    uploadUrl: string,
    metadataUrl: string,
    filePath: string,
    chunkSize: number
  ): void;
};

const { BackgroundUpload } = NativeModules;

export default BackgroundUpload as BackgroundUploadType;
