import {
  EmitterSubscription,
  NativeModules,
  NativeEventEmitter,
} from 'react-native';

type WorkerSubscription = {
  workId: number;
};

type NetworkTask = {
  url: string;
  method: 'GET' | 'POST';
  // headers?: { [key: string]: string };
  authorization?: string;
  data?: string;
};

const BackgroundUploadModule = NativeModules.BackgroundUpload;
const BackgroundUploadEmitter = new NativeEventEmitter(BackgroundUploadModule);

class RNBackgroundUpload {
  startBackgroundUploadVideo = (
    key: number,
    uploadUrl: string,
    metadataUrl: string,
    filePath: string,
    chunkSize: number,
    enableCompression: boolean,
    chainTask: NetworkTask | null
  ): void => {
    BackgroundUploadModule.startBackgroundUploadVideo(
      key,
      uploadUrl,
      metadataUrl,
      filePath,
      chunkSize,
      enableCompression,
      chainTask
    );
  };

  stopBackgroundUpload = (workId: number): void => {
    BackgroundUploadModule.stopBackgroundUpload(workId);
  };

  onStart = (fn: (e: WorkerSubscription) => void): EmitterSubscription =>
    BackgroundUploadEmitter.addListener('onStart', fn);

  onTranscode = (fn: (e: WorkerSubscription) => void): EmitterSubscription =>
    BackgroundUploadEmitter.addListener('onTranscode', fn);

  onSplit = (fn: (e: WorkerSubscription) => void): EmitterSubscription =>
    BackgroundUploadEmitter.addListener('onSplit', fn);

  onRequestMetadata = (
    fn: (e: WorkerSubscription) => void
  ): EmitterSubscription =>
    BackgroundUploadEmitter.addListener('onRequestMetadata', fn);

  onUpload = (fn: (e: WorkerSubscription) => void): EmitterSubscription =>
    BackgroundUploadEmitter.addListener('onUpload', fn);

  onChainTasks = (fn: (e: WorkerSubscription) => void): EmitterSubscription =>
    BackgroundUploadEmitter.addListener('onChainTasks', fn);

  onSuccess = (fn: (e: WorkerSubscription) => void): EmitterSubscription =>
    BackgroundUploadEmitter.addListener('onSuccess', fn);

  onFailure = (fn: (e: WorkerSubscription) => void): EmitterSubscription =>
    BackgroundUploadEmitter.addListener('onFailure', fn);

  onCancelled = (fn: (e: WorkerSubscription) => void): EmitterSubscription =>
    BackgroundUploadEmitter.addListener('onCancelled', fn);
}

const BackgroundUpload = new RNBackgroundUpload();

export default BackgroundUpload;
