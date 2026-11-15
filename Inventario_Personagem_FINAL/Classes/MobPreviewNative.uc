class MobPreviewNative extends Object;
native static final function Actor GetWorldActor(int objectID);
native static final function int BindPreview(WindowHandle control, Actor preview, float fov, float radius, float height);
native static final function int ReleasePreview();
native static final function int SetStageBackground(Texture background);
native static final function vector GetPointerState();
native static final function int SetHuntTextureSize(TextureHandle control,int width,int height);
defaultproperties
{
}
