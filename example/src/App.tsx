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
          'https://cdn.bibabo.vn/api/light/v1/video/chunkedUpload/partUpload',
          'https://cdn.bibabo.vn/api/light/v1/video/chunkedUpload/metadata',
          response.uri,
          1024 * 1024 * 2.5,
          true,
          {
            url: 'https://one.bibabo.vn/api/posts',
            method: 'POST',
            authorization:
              'eyJDVCI6MCwiQ0kiOjEsIlVJIjo5MSwiU0UiOiIxNTc0MjUwMzY3MTk1NTY4MiJ9',
            data:
              '{"groupId":12,"topicId":[2026],"type":2,"desc":"Okie","contentType":3,"products":[],"tags":{"products":[],"users":[]},"backgroundId":0,"isSensitive":0,"isAnonymous":0}',
          }
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
