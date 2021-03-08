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
        BackgroundUpload.startBackgroundUpload(
          'https://cdn.bibabo.vn/api/light/v1/video/chunkedUpload/partUpload',
          // response.uri,
          '/Users/ducminh/Library/Developer/CoreSimulator/Devices/40D61EA5-5CC8-4F24-9B9F-E43CA3F4B552/data/Containers/Data/Application/B4314DFC-A31A-477F-A744-20A83613D07D/tmp/trim.F6FC7F59-1B56-450E-8B69-F54D832FAD18.MOV',
          // '/Users/ducminh/Library/Developer/CoreSimulator/Devices/40D61EA5-5CC8-4F24-9B9F-E43CA3F4B552/data/Containers/Data/Application/9BFD828F-B2C1-49E5-AA3E-3F9D46417C7B/Library/Caches/CompressedOutput/1615195255096.mp4',
          '0t2mh1e8fxx.mp4',
          {
            1: 'BlLVoiFjI7HwMNdPmQmd',
            2: 'UKioI6dJ82fHvmUCUFLq',
          },
          1024 * 1024 * 2
        );
        // BackgroundUpload.startEncodingVideo(response.uri)
      }
    });
  };

  return (
    <View style={styles.container}>
      <TouchableOpacity style={styles.box} onPress={onPress}>
        <Text>Click</Text>
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
