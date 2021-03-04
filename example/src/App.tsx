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
          response.uri,
          // '/storage/emulated/0/DCIM/Camera/20201011_140637.mp4',
          '0ysf4wd2nx3.mp4',
          {
            1: 'PVBXN280vckRaeITwviA',
            2: 'h27uI7tILOiejumpYti0',
            // 3: 'mJx59d8IgCTZWvi6GOLz',
            // 4: 'SwXqoEuXzJgPfTopBSvt',
            // 5: 'GPBdISBgKAK7KWJYlLwR',
            // 6: 'UFDublcWa0UzoUpU4kJX',
            // 7: 'KJ7mwI14NTJdBEy7cx7e',
            // 8: 'i1VNOLx1QJacGDp3h8nO',
            // 9: 'eFdkYo2lkbpazN1uqhFR',
          }
        );
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
