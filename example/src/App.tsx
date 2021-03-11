import React from 'react';
import { StyleSheet, View, Text, TouchableOpacity } from 'react-native';
import BackgroundUpload from 'react-native-background-upload';
import {
  ImageLibraryOptions,
  launchImageLibrary,
} from 'react-native-image-picker';

export default function App() {
  const onPress = () => {
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
          'https://localhost/uploadUrl',
          'https://localhost/metaDataUrl',
          response.uri,
          1024 * 1024 * 2.5,
          false,
          null
        );
      }
    });
  };

  return (
    <View style={styles.container}>
      <TouchableOpacity style={styles.box} onPress={onPress}>
        <Text>Pick video and Upload</Text>
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
