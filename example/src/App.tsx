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
        // BackgroundUpload.startBackgroundUpload(
        //   'https://cdn.bibabo.vn/api/light/v1/video/chunkedUpload/partUpload',
        //   response.uri,
        //   // '/storage/emulated/0/DCIM/Camera/20201011_140637.mp4',
        //   '0oylibsytm6.mp4',
        //   {
        //     1: 'KE5EFa64ACYMM4CY9IFj',
        //     2: '5vu1LORYZgvjr1sjkiK5',
        //     3: 'zrCyoYAMeoxhoZlESR1S',
        //     4: 'CfhLnj7MwXxb7Txbsaru',
        //     5: '9niRZeP6ki9jmLdiIc8Z',
        //     6: '3QcBqQHXBDzqhjh8Nx6v',
        //     7: 'iUO8DfaJOjFPtyVZveGw',
        //     8: 'xKzJuKfreM2cTwJTA47Z',
        //     9: 'XrmBwIP8dAYFznEy1fon',
        //     10: '8oQu63JjOwoWd5gVMxst',
        //     11: '6VP8GuJQhM6NDliUI8KM',
        //     12: 'FVGPV3DSXsCjpPQ5vaEE',
        //   }
        // );
        BackgroundUpload.startEncodingVideo(response.uri)
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
