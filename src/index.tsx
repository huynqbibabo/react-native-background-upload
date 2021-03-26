import {
  EmitterSubscription,
  NativeModules,
  NativeEventEmitter,
} from 'react-native';
import type { NetworkTask, StateChangeResponse } from './type';

const STATE = {
  IDLE: 'idle',
  TRANSCODE: 'transcoding',
  SPLIT: 'splitting',
  REQUEST_METADATA: 'requestMetadata',
  UPLOAD: 'uploading',
  CHAIN_TASK: 'chainTaskProcessing',
  SUCCESS: 'success',
  FAILED: 'failed',
  CANCELLED: 'cancelled',
};

const EVENT = {
  onStateChange: 'onStateChange',
};

const BackgroundUploadModule = NativeModules.BackgroundUpload;
const BackgroundUploadEmitter = new NativeEventEmitter(BackgroundUploadModule);

class RNBackgroundUpload {
  startBackgroundUploadVideo = async (
    channelId: number,
    uploadUrl: string,
    metadataUrl: string,
    filePath: string,
    chunkSize: number,
    enableCompression: boolean,
    chainTask: NetworkTask | null
  ): Promise<number> => {
    return await BackgroundUploadModule.startBackgroundUploadVideo(
      channelId,
      uploadUrl,
      metadataUrl,
      filePath,
      chunkSize,
      enableCompression,
      chainTask
    );
  };

  stopBackgroundUpload = async (workId: string): Promise<void> => {
    return await BackgroundUploadModule.stopBackgroundUpload(workId);
  };

  getCurrentState = async (workId: number): Promise<StateChangeResponse> => {
    return await BackgroundUploadModule.getCurrentState(workId);
  };

  onStateChange = (fn: (e: StateChangeResponse) => void): EmitterSubscription =>
    BackgroundUploadEmitter.addListener(EVENT.onStateChange, fn);
}

const BackgroundUpload = new RNBackgroundUpload();

export { STATE, EVENT };
export default BackgroundUpload;
