import React from 'react';
import { StyleSheet, View, Text, TouchableOpacity } from 'react-native';
import BackgroundUpload from 'react-native-background-upload';
import {
  ImageLibraryOptions,
  launchImageLibrary,
} from 'react-native-image-picker';
// import ImagePicker from 'react-native-image-crop-picker';

export default function App() {
  const uploadWorkId = React.useRef(0);

  React.useEffect(() => {
    BackgroundUpload.onStateChange((response) => {
      console.log('onStateChange: ', response);
    });
  }, []);

  const onPressStart = () => {
    uploadWorkId.current = Math.floor(Date.now() / 1000);
    const options: ImageLibraryOptions = {
      videoQuality: 'high',
      mediaType: 'video',
    };
    launchImageLibrary(options, (response) => {
      console.log('Response = ', response);
      if (response.didCancel) {
        console.log('User cancelled photo picker');
      } else if (response.errorCode) {
        console.log('ImagePicker Error: ', response.errorCode);
      } else if (response.uri) {
        BackgroundUpload.startBackgroundUploadVideo(
          uploadWorkId.current,
          // 'https://localhost/uploadUrl',
          // 'https://localhost/metaDataUrl',
          'https://cdn.bibabo.vn/api/light/v1/video/chunkedUpload/partUpload',
          'https://cdn.bibabo.vn/api/light/v1/video/chunkedUpload/metadata',
          response.uri,
          1024 * 1024 * 2.5,
          true,
          null
        ).then((workId: number) => {
          console.log('workId', workId);
        });
      }
    });
  };

  const onPressStop = (): void => {
    BackgroundUpload.stopBackgroundUpload(uploadWorkId.current.toString());
  };

  const onPressState = (): void => {
    BackgroundUpload.getCurrentState(uploadWorkId.current).then((state) => {
      console.log(state);
    });
  };

  return (
    <View style={styles.container}>
      <TouchableOpacity style={styles.box} onPress={onPressStart}>
        <Text>Pick video and Upload</Text>
      </TouchableOpacity>
      <TouchableOpacity style={styles.box} onPress={onPressStop}>
        <Text>Stop Upload</Text>
      </TouchableOpacity>
      <TouchableOpacity style={styles.box} onPress={onPressState}>
        <Text>Get current state</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  image: {
    width: 200,
    height: 200,
    marginVertical: 30,
  },
  box: {
    padding: 16,
    backgroundColor: 'white',
    borderWidth: 1,
    borderRadius: 8,
    borderColor: 'grey',
  },
});
