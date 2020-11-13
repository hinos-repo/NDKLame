#include <jni.h>
#include <string>
#include <android/log.h>
#include <lame.h>
#include <android/log.h>
#include <lame_global_flags.h>

#define LOGE(FORMAT, ...) __android_log_print(ANDROID_LOG_ERROR,"zph",FORMAT,##__VA_ARGS__);

//Mp3Encoder
static lame_global_flags *glf = NULL;
extern "C"
JNIEXPORT void JNICALL
Java_com_mysample_ndklame_Mp3Encoder_init(JNIEnv *env, jclass clazz, jint in_sample_rate,
                                        jint out_channel, jint out_sample_rate, jint out_bitrate,
                                        jint quality) {
    if (glf != NULL) {
        lame_close(glf);
        glf = NULL;
    }
    glf = lame_init();
    lame_set_in_samplerate(glf, in_sample_rate);
    lame_set_num_channels(glf, out_channel);
    lame_set_out_samplerate(glf, out_sample_rate);
    lame_set_brate(glf, out_bitrate);
    lame_set_quality(glf, quality);
    lame_init_params(glf);
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_mysample_ndklame_Mp3Encoder_encodeByte(JNIEnv *env, jclass clazz, jshortArray buffer_l,
                                                jshortArray buffer_r, jint samples, jbyteArray mp3buf) {
    jshort* j_buffer_l = (*env).GetShortArrayElements(buffer_l, NULL);

    jshort* j_buffer_r = (*env).GetShortArrayElements(buffer_r, NULL);

    const jsize mp3buf_size = (*env).GetArrayLength(mp3buf);
    jbyte* j_mp3buf = (*env).GetByteArrayElements(mp3buf, NULL);

    int result = 0;
    if(glf->num_channels == 2)
    {
        result = lame_encode_buffer_interleaved(glf, j_buffer_l, samples/2, (u_char*)j_mp3buf, mp3buf_size);
    } else {
        result = lame_encode_buffer(glf, j_buffer_l, j_buffer_r, samples, (u_char*)j_mp3buf, mp3buf_size);
    }

    (*env).ReleaseShortArrayElements(buffer_l, j_buffer_l, 0);
    (*env).ReleaseShortArrayElements(buffer_r, j_buffer_r, 0);
    (*env).ReleaseByteArrayElements(mp3buf, j_mp3buf, 0);

    return result;
}extern "C"
JNIEXPORT jint JNICALL
Java_com_mysample_ndklame_Mp3Encoder_flush(JNIEnv *env, jclass clazz, jbyteArray mp3buf)
{
    const jsize mp3buf_size = (*env).GetArrayLength(mp3buf);
    jbyte* j_mp3buf = (*env).GetByteArrayElements(mp3buf, NULL);

    int result = lame_encode_flush(glf, (u_char*)j_mp3buf, mp3buf_size);

    (*env).ReleaseByteArrayElements(mp3buf, j_mp3buf, 0);

    return result;
}extern "C"
JNIEXPORT jint JNICALL
Java_com_mysample_ndklame_Mp3Encoder_encodeMp3(JNIEnv *env, jclass clazz, jstring pcm_path, jstring mp3_path) {
    const char *pcm_path_=env->GetStringUTFChars(pcm_path,0);
    const char *mp3_path_=env->GetStringUTFChars(mp3_path,0);

    FILE *pcm_file;
    FILE *mp3_file;

    int result=-1;

    try {
        pcm_file = fopen(pcm_path_,"rb");
        if(pcm_file!= NULL){
            mp3_file=fopen(mp3_path_,"wb");
        }
    }catch(int a){
        result = -1;
        return result;
    }

    int bufferSize = 1024 * 256;
    short *buffer = new short[bufferSize / 2]; // PCM 버퍼
    short *leftBuffer = new short[bufferSize / 4]; // PCM 왼쪽 버퍼
    short *rightBuffer = new short[bufferSize / 4]; // MP3 오른쪽 버퍼c
    unsigned char* mp3_buffer = new unsigned char[bufferSize];
    size_t readBufferSize = 0;
    do{
        // bufferSize / 2 (count) = 128바이트, bitRate = 128
        readBufferSize = fread(buffer, 2, bufferSize / 2, pcm_file);
        for (int i = 0; i < readBufferSize; i++)
        {
            if (i % 2 == 0) {
                leftBuffer[i / 2] = buffer[i];
            } else {
                rightBuffer[i / 2] = buffer[i];
            }
        }
        //lame_encode_buffer(glf, 왼쪽 버퍼, 오른쪽 버퍼, 샘플, mp3버퍼, mp3버퍼 사이즈)
        size_t wroteSize = lame_encode_buffer(glf, (short int *) leftBuffer, (short int *) rightBuffer, (int)(readBufferSize / 2), mp3_buffer, bufferSize);
        fwrite(mp3_buffer, 1, wroteSize, mp3_file);
    }
    while(readBufferSize > 0);

    delete [] buffer;
    delete [] leftBuffer;
    delete [] rightBuffer;
    delete [] mp3_buffer;

    return result;
}