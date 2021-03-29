export type Event = 'onStateChange';

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

export type CurrentStateInfo = {
  state: State;
  response: string;
};

export type StateChangeResponse = CurrentStateInfo & {
  workId: number;
  progress: number;
};

export type NetworkTask = {
  url: string;
  method: 'GET' | 'POST';
  // headers?: { [key: string]: string };
  authorization?: string;
  data?: string;
};
