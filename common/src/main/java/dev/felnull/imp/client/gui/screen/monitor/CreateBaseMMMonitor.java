package dev.felnull.imp.client.gui.screen.monitor;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.felnull.fnjl.util.FNURLUtil;
import dev.felnull.imp.IamMusicPlayer;
import dev.felnull.imp.client.gui.IIMPSmartRender;
import dev.felnull.imp.client.gui.components.ImageSetButton;
import dev.felnull.imp.client.gui.components.SmartButton;
import dev.felnull.imp.client.gui.screen.MusicManagerScreen;
import dev.felnull.imp.blockentity.MusicManagerBlockEntity;
import dev.felnull.imp.client.renderer.PlayImageRenderer;
import dev.felnull.imp.client.util.FileChooserUtil;
import dev.felnull.imp.music.resource.ImageInfo;
import dev.felnull.otyacraftengine.client.util.OERenderUtil;
import dev.felnull.otyacraftengine.util.OEImageUtil;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public abstract class CreateBaseMMMonitor extends MusicManagerMonitor {
    private static final Gson GSON = new Gson();
    private static final ResourceLocation SET_IMAGE_TEXTURE = new ResourceLocation(IamMusicPlayer.MODID, "textures/gui/container/music_manager/monitor/image_set_base.png");
    private static final Component IMAGE_TEXT = new TranslatableComponent("imp.text.image");
    private static final Component NO_IMAGE_TEXT = new TranslatableComponent("imp.text.noImage");
    private static final Component DROP_INFO_TEXT = new TranslatableComponent("imp.text.dropInfo");
    private static final Component NAME_TEXT = new TranslatableComponent("imp.text.name");
    private static final Component BACK_TEXT = new TranslatableComponent("imp.button.back");
    private static final Component CREATE_TEXT = new TranslatableComponent("imp.button.create");
    private Component NOT_ENTERED_TEXT;
    private EditBox imageUrlEditBox;
    private EditBox nameEditBox;
    private SmartButton createButton;
    private Component imageSetInfo;
    private ImageUrlLoader imageUrlLoader;
    private ImageUploader imageUploader;
    private List<Component> lastNotEnteredTexts;


    public CreateBaseMMMonitor(MusicManagerBlockEntity.MonitorType type, MusicManagerScreen screen) {
        super(type, screen);
    }

    @Override
    public void init(int leftPos, int topPos) {
        super.init(leftPos, topPos);

        addRenderWidget(new ImageSetButton(getStartX() + 149, getStartY() + 22, ImageSetButton.ImageSetType.DELETE, n -> setImage(ImageInfo.EMPTY), getScreen()));

        addRenderWidget(new ImageSetButton(getStartX() + 112, getStartY() + 22, ImageSetButton.ImageSetType.FILE_OPEN, n -> openImage(FileChooserUtil.openImageFileChooser(false)), getScreen()));

        addRenderWidget(new ImageSetButton(getStartX() + 75, getStartY() + 22, ImageSetButton.ImageSetType.PLAYER_FACE, n -> setImage(new ImageInfo(ImageInfo.ImageType.PLAYER_FACE, IIMPSmartRender.mc.player.getGameProfile().getName())), getScreen()));

        this.imageUrlEditBox = new EditBox(IIMPSmartRender.mc.font, getStartX() + 112, getStartY() + 42, 69, 12, new TranslatableComponent("imp.editBox.imageUrl"));
        this.imageUrlEditBox.setMaxLength(300);
        this.imageUrlEditBox.setValue(getImageURL());
        this.imageUrlEditBox.setResponder(this::setImageURL);
        addRenderWidget(this.imageUrlEditBox);

        addRenderWidget(new ImageSetButton(getStartX() + 75, getStartY() + 41, ImageSetButton.ImageSetType.URL, n -> {
            if (this.imageUrlEditBox.getValue().isEmpty()) {
                imageSetInfo = new TranslatableComponent("imp.text.imageLoad.empty");
                return;
            }
            startImageUrlLoad(this.imageUrlEditBox.getValue());
        }, getScreen()));

        this.nameEditBox = new EditBox(IIMPSmartRender.mc.font, getStartX() + 5, getStartY() + 112, 177, 12, new TranslatableComponent("imp.editBox.name"));
        this.nameEditBox.setMaxLength(300);
        this.nameEditBox.setValue(getName());
        this.nameEditBox.setResponder(this::setName);
        addRenderWidget(this.nameEditBox);

        addRenderWidget(new SmartButton(getStartX() + 5, getStartY() + 180, 87, 15, BACK_TEXT, n -> insMonitor(MusicManagerBlockEntity.MonitorType.ADD_PLAY_LIST)));

        this.createButton = addRenderWidget(new SmartButton(getStartX() + 95, getStartY() + 180, 87, 15, CREATE_TEXT, n -> {
            if (getScreen().getBlockEntity() instanceof MusicManagerBlockEntity musicManagerBlockEntity) {
                if (canCreate(musicManagerBlockEntity))
                    create(getImage(), getName());
                insMonitor(MusicManagerBlockEntity.MonitorType.PLAY_LIST);
            }
        }));
        if (getScreen().getBlockEntity() instanceof MusicManagerBlockEntity musicManagerBlockEntity)
            this.createButton.active = canCreate(musicManagerBlockEntity);


    }

    @Override
    public void render(PoseStack poseStack, float f, int mouseX, int mouseY) {
        super.render(poseStack, f, mouseX, mouseY);
        OERenderUtil.drawTexture(SET_IMAGE_TEXTURE, poseStack, getStartX(), getStartY(), 0f, 0f, width, height, width, height);
        drawSmartText(poseStack, IMAGE_TEXT, getStartX() + 5, getStartY() + 13);

        if (!getImage().isEmpty()) {
            PlayImageRenderer.getInstance().draw(getImage(), poseStack, getStartX() + 6, getStartY() + 23, 64, false);
        } else {
            drawSmartCenterText(poseStack, NO_IMAGE_TEXT, getStartX() + 38, getStartY() + 51);
        }

        drawSmartText(poseStack, DROP_INFO_TEXT, getStartX() + 5, getStartY() + 90);
        if (imageSetInfo != null)
            drawSmartFixedWidthText(poseStack, imageSetInfo, getStartX() + 75, getStartY() + 59, 107);

        drawSmartText(poseStack, NAME_TEXT, getStartX() + 5, getStartY() + 102);

        if (NOT_ENTERED_TEXT != null)
            drawSmartFixedWidthText(poseStack, NOT_ENTERED_TEXT, getStartX() + 5, getStartY() + 171, 177, 0XFFFF6347);

    }

    public abstract void create(ImageInfo imageInfo, String name);

    public boolean canCreate(MusicManagerBlockEntity blockEntity) {
        return !getName(blockEntity).isEmpty();
    }

    public List<Component> getNotEntered(List<Component> names, MusicManagerBlockEntity blockEntity) {
        if (getName(blockEntity).isEmpty())
            names.add(NAME_TEXT);
        return names;
    }

    private void updateNotEnteredText() {
        if (getScreen().getBlockEntity() instanceof MusicManagerBlockEntity musicManagerBlockEntity) {
            List<Component> notEnteredTexts = getNotEntered(new ArrayList<>(), musicManagerBlockEntity);
            if (!notEnteredTexts.equals(lastNotEnteredTexts)) {
                if (!canCreate(musicManagerBlockEntity)) {
                    StringBuilder sb = new StringBuilder();
                    for (Component notEnteredText : notEnteredTexts) {
                        sb.append(notEnteredText.getString()).append(", ");
                    }
                    sb.deleteCharAt(sb.length() - 1);
                    sb.deleteCharAt(sb.length() - 1);
                    NOT_ENTERED_TEXT = new TranslatableComponent("imp.text.notEntered", sb.toString());
                    this.createButton.active = false;
                } else {
                    NOT_ENTERED_TEXT = null;
                    this.createButton.active = true;
                }
                lastNotEnteredTexts = notEnteredTexts;
            }
        } else {
            NOT_ENTERED_TEXT = null;
            this.createButton.active = false;
        }
    }

    @Override
    public void renderAppearance(MusicManagerBlockEntity blockEntity, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, int j, float f, float monitorWidth, float monitorHeight) {
        super.renderAppearance(blockEntity, poseStack, multiBufferSource, i, j, f, monitorWidth, monitorHeight);
        float onPxW = monitorWidth / (float) width;
        float onPxH = monitorHeight / (float) height;
        OERenderUtil.renderTextureSprite(SET_IMAGE_TEXTURE, poseStack, multiBufferSource, 0, 0, OERenderUtil.MIN_BREADTH * 2, 0, 0, 0, monitorWidth, monitorHeight, 0, 0, width, height, width, height, i, j);

        renderSmartTextSprite(poseStack, multiBufferSource, IMAGE_TEXT, 5, 13, OERenderUtil.MIN_BREADTH * 4, onPxW, onPxH, monitorHeight, i);

        var img = getImage(blockEntity);
        float sc = onPxW / onPxH;
        float sch = onPxH / onPxW;
        if (!img.isEmpty()) {
            poseStack.pushPose();
            poseStack.scale(sc, 1, 1);
            PlayImageRenderer.getInstance().renderSprite(img, poseStack, multiBufferSource, (6f * onPxW) / sc, monitorHeight - (64 + 23) * onPxH, OERenderUtil.MIN_BREADTH * 4, 64 * onPxH, i, j, false);
            poseStack.popPose();
        } else {
            int strl = IIMPSmartRender.mc.font.width(NO_IMAGE_TEXT);
            renderSmartTextSprite(poseStack, multiBufferSource, NO_IMAGE_TEXT, 6 + ((38f * sc) - (float) strl / 2f), 51, OERenderUtil.MIN_BREADTH * 4, onPxW, onPxH, monitorHeight, i);
        }

        renderSmartButtonSprite(poseStack, multiBufferSource, 149, 22, OERenderUtil.MIN_BREADTH * 3, 33, 15, i, j, onPxW, onPxH, monitorHeight, MusicManagerMonitor.WIDGETS_TEXTURE, 73 + 11, 19, 11, 11, 256, 256);

        renderSmartButtonSprite(poseStack, multiBufferSource, 112, 22, OERenderUtil.MIN_BREADTH * 3, 33, 15, i, j, onPxW, onPxH, monitorHeight, MusicManagerMonitor.WIDGETS_TEXTURE, 73, 19, 11, 11, 256, 256);

        renderSmartButtonBoxSprite(poseStack, multiBufferSource, 75, 22, OERenderUtil.MIN_BREADTH * 3, 33, 15, i, j, onPxW, onPxH, monitorHeight);
        OERenderUtil.renderPlayerFaceSprite(poseStack, multiBufferSource, IIMPSmartRender.mc.player.getGameProfile().getId(), onPxW * (75f + (33f - 11f) / 2f), monitorHeight - (onPxH * (22f + ((15f - 11f) / 2f) * sch)) - 11 * onPxW, OERenderUtil.MIN_BREADTH * 5, 0, 0, 0, 11 * onPxW, i, j);

        renderSmartButtonSprite(poseStack, multiBufferSource, 75, 41, OERenderUtil.MIN_BREADTH * 4, 33, 15, i, j, onPxW, onPxH, monitorHeight, MusicManagerMonitor.WIDGETS_TEXTURE, 73 + 22, 19, 11, 11, 256, 256);

        renderSmartEditBoxSprite(poseStack, multiBufferSource, 112, 42, OERenderUtil.MIN_BREADTH * 3, 69, 12, i, j, onPxW, onPxH, monitorHeight, getImageURL(blockEntity));

        renderSmartTextSprite(poseStack, multiBufferSource, DROP_INFO_TEXT, 5, 90, OERenderUtil.MIN_BREADTH * 3, onPxW, onPxH, monitorHeight, i);

        renderSmartTextSprite(poseStack, multiBufferSource, NAME_TEXT, 5, 102, OERenderUtil.MIN_BREADTH * 3, onPxW, onPxH, monitorHeight, i);

        renderSmartEditBoxSprite(poseStack, multiBufferSource, 5, 112, OERenderUtil.MIN_BREADTH * 3, 177, 12, i, j, onPxW, onPxH, monitorHeight, getName(blockEntity));

        if (!canCreate(blockEntity)) {
            StringBuilder sb = new StringBuilder();
            for (Component notEnteredText : getNotEntered(new ArrayList<>(), blockEntity)) {
                sb.append(notEnteredText.getString()).append(", ");
            }
            sb.deleteCharAt(sb.length() - 1);
            sb.deleteCharAt(sb.length() - 1);
            renderSmartTextSpriteColor(poseStack, multiBufferSource, new TranslatableComponent("imp.text.notEntered", sb.toString()), 5, 171, OERenderUtil.MIN_BREADTH * 3, onPxW, onPxH, monitorHeight, 0XFFFF6347, i);
        }

        renderSmartButtonSprite(poseStack, multiBufferSource, 5, 180, OERenderUtil.MIN_BREADTH * 3, 87, 15, i, j, onPxW, onPxH, monitorHeight, BACK_TEXT, true);
        renderSmartButtonSprite(poseStack, multiBufferSource, 95, 180, OERenderUtil.MIN_BREADTH * 3, 87, 15, i, j, onPxW, onPxH, monitorHeight, CREATE_TEXT, true, !canCreate(blockEntity));
    }


    @Override
    protected MusicManagerBlockEntity.MonitorType getParentType() {
        return MusicManagerBlockEntity.MonitorType.ADD_PLAY_LIST;
    }

    @Override
    public void depose() {
        super.depose();
        stopImageUrlLoad();
        stopImageUpload();
    }

    @Override
    public void onFilesDrop(List<Path> list) {
        File[] files = new File[list.size()];
        for (int i = 0; i < list.size(); i++) {
            files[i] = list.get(i).toFile();
        }
        openImage(files);
    }

    public String getName() {
        if (getScreen().getBlockEntity() instanceof MusicManagerBlockEntity musicManagerBlockEntity)
            return getName(musicManagerBlockEntity);
        return "";
    }

    public String getName(MusicManagerBlockEntity musicManagerBlockEntity) {
        return musicManagerBlockEntity.getMyCreateName();
    }

    private void setName(String name) {
        getScreen().insCreateName(name);
    }

    public ImageInfo getImage() {
        if (getScreen().getBlockEntity() instanceof MusicManagerBlockEntity musicManagerBlockEntity)
            return getImage(musicManagerBlockEntity);
        return ImageInfo.EMPTY;
    }

    private ImageInfo getImage(MusicManagerBlockEntity musicManagerBlockEntity) {
        return musicManagerBlockEntity.getMyImage();
    }

    private void setImage(ImageInfo image) {
        getScreen().insImage(image);
    }

    private String getImageURL() {
        if (getScreen().getBlockEntity() instanceof MusicManagerBlockEntity musicManagerBlockEntity)
            return getImageURL(musicManagerBlockEntity);
        return "";
    }

    private String getImageURL(MusicManagerBlockEntity musicManagerBlockEntity) {
        return musicManagerBlockEntity.getMyImageURL();
    }

    private void setImageURL(String text) {
        getScreen().insImageURL(text);
    }

    private void startImageUrlLoad(String url) {
        stopImageUrlLoad();
        imageSetInfo = new TranslatableComponent("imp.text.imageLoad.loadingImage");
        imageUrlLoader = new ImageUrlLoader(url);
        imageUrlLoader.start();
    }

    private void stopImageUrlLoad() {
        if (imageUrlLoader != null) {
            imageUrlLoader.interrupt();
            imageUrlLoader = null;
        }
        imageSetInfo = null;
    }

    private void startImageUpload(byte[] data) {
        stopImageUpload();
        imageSetInfo = new TranslatableComponent("imp.text.imageLoad.loadingImage");
        imageUploader = new ImageUploader(data);
        imageUploader.start();
    }


    private void stopImageUpload() {
        if (imageUploader != null) {
            imageUploader.interrupt();
            imageUploader = null;
        }
        imageSetInfo = null;
    }

    private void openImage(File[] files) {
        if (files == null || files.length == 0) return;
        if (files.length != 1) {
            imageSetInfo = new TranslatableComponent("imp.text.imageLoad.tooManyImages");
            return;
        }
        imageSetInfo = null;
        File file = files[0];
        if (file.exists()) {
            try {
                startImageUpload(Files.readAllBytes(file.toPath()));
            } catch (IOException e) {
                imageSetInfo = new TranslatableComponent("imp.text.imageLoad.error", e.getLocalizedMessage());
                e.printStackTrace();
            }
        } else {
            imageSetInfo = new TranslatableComponent("imp.text.imageLoad.fileNotFound");
        }
    }

    @Override
    public void tick() {
        super.tick();
        updateNotEnteredText();
    }

    private class ImageUrlLoader extends Thread {
        private final String url;

        private ImageUrlLoader(String url) {
            this.url = url;
        }

        @Override
        public void run() {
            try {
                var urll = FNURLUtil.getConnection(new URL(url));
                long max = 3145728L;
                if (urll.getContentLengthLong() > max) {
                    startImageUpload(urll.getInputStream().readAllBytes());
                    return;
                }
                byte[] img = urll.getInputStream().readAllBytes();
                if (!OEImageUtil.isImage(img)) {
                    imageSetInfo = new TranslatableComponent("imp.text.imageLoad.notImageUrl");
                    return;
                }
                setImage(new ImageInfo(ImageInfo.ImageType.URL, url));
                imageSetInfo = null;
            } catch (Exception e) {
                imageSetInfo = new TranslatableComponent("imp.text.imageLoad.error", e.getLocalizedMessage());
                e.printStackTrace();
            }
        }
    }

    private class ImageUploader extends Thread {
        private byte[] data;

        private ImageUploader(byte[] data) {
            this.data = data;
        }

        @Override
        public void run() {
            try {
                if (!OEImageUtil.isImage(data)) {
                    imageSetInfo = new TranslatableComponent("imp.text.imageLoad.notImage");
                    return;
                }
                long max = 3145728L;
                if (data.length > max) {
                    imageSetInfo = new TranslatableComponent("imp.text.imageLoad.optimizationImage");
                    data = OEImageUtil.reductionSize(data, max - 100);
                }
                imageSetInfo = new TranslatableComponent("imp.text.imageLoad.uploadImage");
                Files.write(Paths.get("test.gif"), data);

                String url;
                try {
                    url = uploadToImgur(data);
                } catch (IOException e) {
                    imageSetInfo = new TranslatableComponent("imp.text.imageLoad.uploadFailure", e.getMessage());
                    e.printStackTrace();
                    return;
                }

                setImage(new ImageInfo(ImageInfo.ImageType.URL, url));
                imageSetInfo = null;
            } catch (Exception e) {
                imageSetInfo = new TranslatableComponent("imp.text.imageLoad.error", e.getLocalizedMessage());
                e.printStackTrace();
            }
        }
    }

    private String uploadToImgur(byte[] data) throws IOException, InterruptedException {
        HttpClient hc = HttpClient.newHttpClient();
        HttpRequest hr = HttpRequest.newBuilder(URI.create("https://api.imgur.com/3/image"))
                .POST(HttpRequest.BodyPublishers.ofByteArray(data))
                .header("Authorization", "Client-ID 9a0189f3c8b74b9")
                .build();
        HttpResponse<String> res = hc.send(hr, HttpResponse.BodyHandlers.ofString());
        JsonObject upData = GSON.fromJson(res.body(), JsonObject.class);
        if (upData.getAsJsonObject("data") == null || upData.getAsJsonObject("data").get("link") == null)
            throw new IOException("code " + upData.get("status").getAsInt());
        return upData.getAsJsonObject("data").get("link").getAsString();
    }
}
