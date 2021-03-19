import {
  EmitterSubscription,
  NativeModules,
  NativeEventEmitter,
} from 'react-native';

export type WorkerSubscription = {
  channelId: number;
  workId: number;
  state: string;
};

export type WorkerProgress = Omit<WorkerSubscription, 'state'> & {
  progress?: number;
  response?: string;
  status: string;
};

export type WorkerResponse = Omit<WorkerProgress, 'progress'> & {
  response: string;
};

export type NetworkTask = {
  url: string;
  method: 'GET' | 'POST';
  // headers?: { [key: string]: string };
  authorization?: string;
  data?: string;
};

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

  stopBackgroundUpload = async (workId: number): Promise<void> => {
    return await BackgroundUploadModule.stopBackgroundUpload(workId);
  };

  getCurrentState = async (
    channelId: number,
    workId: number
  ): Promise<string> => {
    return await BackgroundUploadModule.getCurrentState(channelId, workId);
  };

  onStateChange = (fn: (e: WorkerSubscription) => void): EmitterSubscription =>
    BackgroundUploadEmitter.addListener(EVENT.onStateChange, fn);

  onTranscoding = (fn: (e: WorkerProgress) => void): EmitterSubscription =>
    BackgroundUploadEmitter.addListener(EVENT.onTranscoding, fn);

  onRequestMetadata = (fn: (e: WorkerResponse) => void): EmitterSubscription =>
    BackgroundUploadEmitter.addListener(EVENT.onRequestMetadata, fn);

  onUploading = (fn: (e: WorkerProgress) => void): EmitterSubscription =>
    BackgroundUploadEmitter.addListener(EVENT.onUploading, fn);

  onChainTasks = (fn: (e: WorkerSubscription) => void): EmitterSubscription =>
    BackgroundUploadEmitter.addListener(EVENT.onChainTask, fn);

  onSuccess = (fn: (e: WorkerSubscription) => void): EmitterSubscription =>
    BackgroundUploadEmitter.addListener(EVENT.onSuccess, fn);

  onFailure = (fn: (e: WorkerSubscription) => void): EmitterSubscription =>
    BackgroundUploadEmitter.addListener(EVENT.onFailure, fn);

  onCancelled = (fn: (e: WorkerSubscription) => void): EmitterSubscription =>
    BackgroundUploadEmitter.addListener(EVENT.onCancelled, fn);
}

const BackgroundUpload = new RNBackgroundUpload();

export { STATE, EVENT };
export default BackgroundUpload;
