#define CLASSIC_TIMESTAMP 0x6acabdc8
#define CLASSIC_IMAGE_SIZE 0x9e7000
#define CLASSIC_SCENE_RVA 0x1da99
#define CLASSIC_ALLOW_RVA 0x1dab3
#define CLASSIC_SHADOW_FLAG_RVA 0x8c038
static const u8 classic_scene_sig[24]={131,61,56,192,8,16,0,116,17,161,180,21,158,16,133,192,116,8,59,240,15,132,45,10};
static const u8 classic_scene_mask[24]={1,1,0,0,0,0,1,1,1,1,0,0,0,0,1,1,1,1,1,1,1,1,1,1};