export type Event =
  | 'onStateChange'
  | 'onTranscoding'
  | 'onRequestMetadata'
  | 'onUploading'
  | 'onChainTask'
  | 'onSuccess'
  | 'onFailure'
  | 'onCancelled';

export type State =
  | 'idle'
  | 'transcoding'
  | 'splitting'
  | 'requestMetadata'
  | 'uploading'
  | 'chainTaskProcessing'
  | 'success'
  | 'failed'
  | 'cancelled';

export type StateChangeResponse = {
  channelId: number;
  workId: number;
  state: State;
};

export type TranscodingProcess = Omit<StateChangeResponse, 'state'> & {
  progress: number;
  status: string;
};

export type RequestMetaDataProcess = Omit<StateChangeResponse, 'state'> & {
  response: string;
  status: string;
};

export type UploadProcess = TranscodingProcess & {
  response: string;
};

export type ChainTaskProcess = RequestMetaDataProcess;

export type SuccessStateResponse = Omit<StateChangeResponse, 'state'>;

export type FailureStateResponse = Omit<StateChangeResponse, 'state'> & {
  error: string;
};

export type CancelledStateResponse = Omit<StateChangeResponse, 'state'> & {
  previousState: State;
};

export type NetworkTask = {
  url: string;
  method: 'GET' | 'POST';
  // headers?: { [key: string]: string };
  authorization?: string;
  data?: string;
};
