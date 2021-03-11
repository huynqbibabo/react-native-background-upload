import { NativeModules } from 'react-native';

type NetworkTask = {
  url: string;
  method: 'GET' | 'POST';
  // headers?: { [key: string]: string };
  authorization?: string;
  data?: string;
};

type BackgroundUploadType = {
  startBackgroundUploadVideo(
    uploadUrl: string,
    metadataUrl: string,
    filePath: string,
    chunkSize: number,
    enableCompression: boolean,
    chainTask: NetworkTask | null
  ): void;
};

const { BackgroundUpload } = NativeModules;

export default BackgroundUpload as BackgroundUploadType;
