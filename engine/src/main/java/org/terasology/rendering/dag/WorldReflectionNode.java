/*
 * Copyright 2016 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.rendering.dag;

import org.lwjgl.opengl.GL11;
import org.terasology.config.Config;
import org.terasology.config.RenderingConfig;
import org.terasology.context.Context;
import org.terasology.monitoring.PerformanceMonitor;
import org.terasology.rendering.assets.material.Material;
import org.terasology.rendering.assets.shader.ShaderProgramFeature;
import org.terasology.rendering.backdrop.BackdropRenderer;
import org.terasology.rendering.cameras.Camera;
import org.terasology.rendering.opengl.FBO;
import org.terasology.rendering.opengl.FrameBuffersManager;
import org.terasology.rendering.opengl.OpenGLUtils;
import org.terasology.rendering.primitives.ChunkMesh;
import org.terasology.rendering.world.RenderQueuesHelper;
import org.terasology.rendering.world.WorldRenderer;
import org.terasology.rendering.world.WorldRendererImpl;

import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.terasology.rendering.opengl.OpenGLUtils.bindDisplay;

/**
 * Diagram of this node can be viewed from:
 * TODO: move diagram to the wiki when this part of the code is stable
 * - https://docs.google.com/drawings/d/1Iz7MA8Y5q7yjxxcgZW-0antv5kgx6NYkvoInielbwGU/edit?usp=sharing
 */
public class WorldReflectionNode implements Node {

    private RenderQueuesHelper renderQueues;
    private RenderingConfig renderingConfig;
    private WorldRenderer worldRenderer;
    private Material chunkShader;

    private Context context;
    private BackdropRenderer backdropRenderer;
    private Camera playerCamera;

    private FBO sceneReflected;
    private FBO sceneOpaque;

    public WorldReflectionNode(Context context, Camera playerCamera, Material chunkShader) {
        this.context = context;
        this.backdropRenderer = context.get(BackdropRenderer.class);
        this.playerCamera = playerCamera;
        this.renderingConfig = context.get(Config.class).getRendering();
        this.chunkShader = chunkShader;
        this.worldRenderer = context.get(WorldRenderer.class);
        this.renderQueues = context.get(RenderQueuesHelper.class);
    }

    @Override
    public void update(float deltaInSeconds) {
        this.sceneReflected = context.get(FrameBuffersManager.class).getFBO("sceneReflected");
        this.sceneOpaque = context.get(FrameBuffersManager.class).getFBO("sceneOpaque");
    }

    @Override
    public void process() {
        PerformanceMonitor.startActivity("Render World (Reflection)");

        sceneReflected.bind();
        OpenGLUtils.setViewportToSizeOf(sceneReflected);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        GL11.glCullFace(GL11.GL_FRONT);
        playerCamera.setReflected(true);


        playerCamera.lookThroughNormalized(); // we don't want the reflected scene to be bobbing or moving with the player
        backdropRenderer.render(playerCamera);
        playerCamera.lookThrough();

        if (renderingConfig.isReflectiveWater()) {
            chunkShader.activateFeature(ShaderProgramFeature.FEATURE_USE_FORWARD_LIGHTING);
            worldRenderer.renderChunks(renderQueues.chunksOpaqueReflection, ChunkMesh.RenderPhase.OPAQUE, playerCamera, WorldRendererImpl.ChunkRenderMode.REFLECTION);
            chunkShader.deactivateFeature(ShaderProgramFeature.FEATURE_USE_FORWARD_LIGHTING);
        }

        playerCamera.setReflected(false);

        GL11.glCullFace(GL11.GL_BACK);
        bindDisplay();
        OpenGLUtils.setViewportToSizeOf(sceneOpaque); // toWholeDisplay


        PerformanceMonitor.endActivity();
    }
}
