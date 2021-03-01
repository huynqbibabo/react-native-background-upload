import { NativeModules } from 'react-native';

type BackgroundUploadType = {
  multiply(a: number, b: number): Promise<number>;
};

const { BackgroundUpload } = NativeModules;

export default BackgroundUpload as BackgroundUploadType;
