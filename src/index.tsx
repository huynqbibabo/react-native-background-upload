import {
  EmitterSubscription,
  NativeModules,
  NativeEventEmitter,
} from 'react-native';
import type {
  CancelledStateResponse,
  ChainTaskProcess,
  FailureStateResponse,
  NetworkTask,
  RequestMetaDataProcess,
  StateChangeResponse,
  SuccessStateResponse,
  TranscodingProcess,
  UploadProcess,
} from './type';

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
  onTranscoding: 'onTranscoding',
  onRequestMetadata: 'onRequestMetadata',
  onUploading: 'onUploading',
  onChainTask: 'onChainTask',
  onSuccess: 'onSuccess',
  onFailure: 'onFailure',
  onCancelled: 'onCancelled',
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

  getCurrentState = async (workId: number): Promise<string> => {
    return await BackgroundUploadModule.getCurrentState(workId);
  };

  onStateChange = (fn: (e: StateChangeResponse) => void): EmitterSubscription =>
    BackgroundUploadEmitter.addListener(EVENT.onStateChange, fn);

  onTranscoding = (fn: (e: TranscodingProcess) => void): EmitterSubscription =>
    BackgroundUploadEmitter.addListener(EVENT.onTranscoding, fn);

  onRequestMetadata = (
    fn: (e: RequestMetaDataProcess) => void
  ): EmitterSubscription =>
    BackgroundUploadEmitter.addListener(EVENT.onRequestMetadata, fn);

  onUploading = (fn: (e: UploadProcess) => void): EmitterSubscription =>
    BackgroundUploadEmitter.addListener(EVENT.onUploading, fn);

  onChainTasks = (fn: (e: ChainTaskProcess) => void): EmitterSubscription =>
    BackgroundUploadEmitter.addListener(EVENT.onChainTask, fn);

  onSuccess = (fn: (e: SuccessStateResponse) => void): EmitterSubscription =>
    BackgroundUploadEmitter.addListener(EVENT.onSuccess, fn);

  onFailure = (fn: (e: FailureStateResponse) => void): EmitterSubscription =>
    BackgroundUploadEmitter.addListener(EVENT.onFailure, fn);

  onCancelled = (
    fn: (e: CancelledStateResponse) => void
  ): EmitterSubscription =>
    BackgroundUploadEmitter.addListener(EVENT.onCancelled, fn);
}

const BackgroundUpload = new RNBackgroundUpload();

export { STATE, EVENT };
export default BackgroundUpload;
