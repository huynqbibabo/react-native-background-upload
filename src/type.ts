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
  workId: number;
  state: State;
  response: string;
  progress: number;
};

export type NetworkTask = {
  url: string;
  method: 'GET' | 'POST';
  // headers?: { [key: string]: string };
  authorization?: string;
  data?: string;
};
