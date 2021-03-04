import { NativeModules } from 'react-native';

type HashMap = { [key: string]: string };

type BackgroundUploadType = {
  startBackgroundUpload(
    url: string,
    filePath: string,
    fileName: string,
    hash: HashMap
  ): void;
};

const { BackgroundUpload } = NativeModules;

export default BackgroundUpload as BackgroundUploadType;
