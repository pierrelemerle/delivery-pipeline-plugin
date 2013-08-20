package com.diabol.pipefitter;

import com.diabol.pipefitter.model.Pipeline;
import com.diabol.pipefitter.model.Stage;
import com.diabol.pipefitter.model.Task;
import hudson.Extension;
import hudson.model.*;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import static com.beust.jcommander.internal.Lists.newArrayList;
import static hudson.model.Descriptor.FormException;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableCollection;

/**
 * @author Per Huss <mr.per.huss@gmail.com>
 */
public class PipelineView extends View
{
    private static final Logger LOGGER = Logger.getLogger(PipelineView.class.getName());

    private int refreshFrequency;
    private String title;
    private String numberOfBuilds;
    private Collection<TopLevelItem> items = newArrayList();
    private String firstJob;


    @DataBoundConstructor
    public PipelineView(String name, String title, String firstJob, ViewGroup owner)
    {
        super(name, owner);
        this.title = title;
        this.firstJob = firstJob;
    }

    @Override
    public Collection<TopLevelItem> getItems()
    {
        return unmodifiableCollection(newArrayList(items));
    }

    @Override
    public boolean contains(TopLevelItem item)
    {
        return false;
    }

    @Override
    public void onJobRenamed(Item item, String oldName, String newName)
    {
        // Replace in model
    }

    @Override
    protected void submit(StaplerRequest req) throws IOException, ServletException, FormException
    {
        req.bindJSON(this, req.getSubmittedForm());
    }

    @Override
    public Item doCreateItem(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException
    {
        return getOwner().getPrimaryView().doCreateItem(req, rsp);
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getFirstJob() {
        return firstJob;
    }

    public void setFirstJob(String firstJob) {
        this.firstJob = firstJob;
    }

    public Pipeline getPipeline()
    {
        AbstractProject first = Jenkins.getInstance().getItem(firstJob, Jenkins.getInstance(), AbstractProject.class);

        List<Stage> stages = newArrayList();
        for(AbstractProject job : getAllDownstreamJobs(first))
        {
            Task task = new Task("Task");
            Stage stage = new Stage(job.getDisplayName(), singletonList(task));
            stages.add(stage);
        }

        return new Pipeline("Hepp", stages);
    }

    private List<AbstractProject> getAllDownstreamJobs(AbstractProject first)
    {
        List<AbstractProject> jobs = newArrayList();
        jobs.add(first);

        List<AbstractProject> sune = first.getDownstreamProjects();
        for (AbstractProject project : sune)
        {
            jobs.addAll(getAllDownstreamJobs(project));
        }

        return jobs;
    }

    @Extension
    public static class DescriptorImpl extends ViewDescriptor
    {
        public String getDisplayName()
        {
            return "Pipeline View";
        }
        /**
         * Display Job List Item in the Edit View Page
         *
         * @param context What to resolve relative job names against?
         * @return ListBoxModel
         */
        public ListBoxModel doFillFirstJobItems(@AncestorInPath ItemGroup<?> context) {
            final hudson.util.ListBoxModel options = new hudson.util.ListBoxModel();
            for (final AbstractProject<?, ?> p : Jenkins.getInstance().getAllItems(AbstractProject.class)) {
                options.add(/* TODO 1.515: p.getRelativeDisplayNameFrom(context) */p.getFullDisplayName(),
                        p.getRelativeNameFrom(context));
            }
            return options;
        }

    }
}
